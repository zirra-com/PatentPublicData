package gov.uspto.patent;

import com.google.common.base.Preconditions;
import gov.uspto.common.file.FileIterator;
import gov.uspto.common.filter.FileFilterChain;
import gov.uspto.common.filter.SuffixFilter;
import gov.uspto.patent.bulk.DumpFileAps;
import gov.uspto.patent.bulk.DumpFileXml;
import gov.uspto.patent.bulk.DumpReader;
import gov.uspto.patent.model.Patent;
import gov.uspto.patent.serialize.DocumentBuilder;
import gov.uspto.patent.serialize.JsonMapper;
import gov.uspto.patent.serialize.JsonMapperFlat;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;

/**
 * Read Bulk Patent Dumps and Export JSON
 * 
 * <pre>
 * Default output is File containing JSON Document per line
 * --input="ipa_corpusApps_2005.zip"
 * </pre>
 * 
 * <pre>
 * Output an Individual File per Record
 * --input="ipa_corpusApps_2005.zip" --outBulk=false
 * </pre>
 * 
 * @author Brian G. Feldman (brian.feldman@uspto.gov)
 *
 */
public class TransformerCli2 {
	private static final Logger LOGGER = LoggerFactory.getLogger(TransformerCli2.class);

	private final DocumentBuilder<Patent> fileBuilder;
	private boolean stdout = false;
	private boolean insertHtmlEntities = false;
	private boolean outputBulkFile;
	private long totalCount = 0;
	private long totalLimit = Long.MAX_VALUE;
	private Writer currentWriter;
	private String currentFileName;

	public TransformerCli2(DocumentBuilder<Patent> fileBuilder, boolean outputBulkFile) {
		this.fileBuilder = fileBuilder;
		this.outputBulkFile = outputBulkFile;
	}

	public TransformerCli2(DocumentBuilder<Patent> fileBuilder) {
		this.fileBuilder = fileBuilder;
		this.stdout = true;
	}

	/**
	 * Add HTML Entities to XML; Only needed for patents within PAP format.
	 */
	public void addHtmlEntities() {
		this.insertHtmlEntities = true;
	}

	public void process(Path inputPath, Path outputDir) throws FileNotFoundException {
		Preconditions.checkNotNull(inputPath, "Input File can not be null");
		File inputFile = inputPath.toFile();

		if (outputDir != null && !outputDir.toFile().exists()) {
			outputDir.toFile().mkdirs();
		}

		if (inputFile.isDirectory()) {
			for (File file : inputFile.listFiles()) {
				process(file.toPath(), outputDir.resolve(file.getName()));
			}
		} else {
			Iterator<File> fileIterator = FileIterator.getFileIterator(inputFile, new String[]{"zip"}, true);

			for (int i = 1; fileIterator.hasNext() && totalCount < totalLimit; i++) {
				File file = fileIterator.next();

				MDC.put("DOCID", file.getName());
				LOGGER.info("Dump File[{}]: {}", i, file.getAbsoluteFile());

				if (outputBulkFile) {
					if (currentWriter != null) {
						try {
							currentWriter.close();
						} catch (IOException e) {
							// close quiet.
						}
					}
					currentWriter = null;
					currentFileName = file.getName().replaceFirst(".zip$", ".bulk");
				}

				try {
					DumpReader dumpReader = read(file);
					processDumpFile(dumpReader, outputDir);
				} catch (IOException e) {
					LOGGER.error("Failed processing Dump file: {}", file.getAbsolutePath(), e);
				}
			}

			if (totalCount >= totalLimit) {
				LOGGER.info("Process Complete, Total Record Limit Reached [{}]", totalCount);
			} else {
				LOGGER.info("Process Complete, Total Records [{}]", totalCount);
			}
		}
	}

	private DumpReader read(File file) {
		PatentDocFormat patentDocFormat = new PatentDocFormatDetect().fromFileName(file);

		FileFilterChain filters = new FileFilterChain();

		DumpReader dumpReader;
		switch (patentDocFormat) {
		case Greenbook:
			dumpReader = new DumpFileAps(file);
			// filters.addRule(new PathFileFilter(""));
			// filters.addRule(new SuffixFilter("txt"));
			break;
		default:
			dumpReader = new DumpFileXml(file);

			if (PatentDocFormat.Pap.equals(patentDocFormat) || insertHtmlEntities) {
				((DumpFileXml) dumpReader).addHTMLEntities();
			}

			// filters.addRule(new PathFileFilter(""));
			filters.addRule(new SuffixFilter("xml", "sgml", "sgm"));
		}

		dumpReader.setFileFilter(filters);

		return dumpReader;
	}

	private void processDumpFile(DumpReader dumpReader, Path outputDir) throws IOException {
		try {
			dumpReader.open();
			PatentReader patentReader = new PatentReader(dumpReader.getPatentDocFormat());

			for (; dumpReader.hasNext() && totalCount < totalLimit; totalCount++) {

				MDC.put("DOCID", dumpReader.getFile().getName() + ":" + dumpReader.getCurrentRecCount());

				String xmlDocStr = dumpReader.next();
				if (xmlDocStr == null) {
					currentWriter.close();
					break;
				}

				try (StringReader rawText = new StringReader(xmlDocStr)) {
					Patent patent = patentReader.read(rawText);
					String patentId = patent.getDocumentId() != null ? patent.getDocumentId().toText() : "";
					MDC.put("DOCID", patentId);

					if (!stdout && !outputBulkFile || outputBulkFile && currentWriter == null) {
						if (!outputBulkFile) {
							currentFileName = patentId + ".json";
							if (currentWriter != null) {
								currentWriter.close();
							}
						}
						currentWriter = new BufferedWriter(new FileWriter(outputDir.resolve(currentFileName).toFile()));
					} else {
						if (!outputBulkFile) {
							currentWriter = new StringWriter();
						}
					}

					LOGGER.info("Record: '{}' from {}:{}", patentId, dumpReader.getFile(),
							dumpReader.getCurrentRecCount());
					LOGGER.trace("Patent Object: " + patent.toString());
					write(patent, currentWriter);
					currentWriter.flush();
					MDC.put("DOCID", "");
				} catch (PatentReaderException e1) {
					LOGGER.error("Patent Reader error: ", e1);
				} catch (IOException e) {
					LOGGER.error("Writer error: ", e);
				}
			}

		} finally {
			dumpReader.close();
		}
	}

	private void write(Patent patent, Writer writer) throws IOException {
		fileBuilder.write(patent, writer);
		if (outputBulkFile) {
			writer.write("\n");
		} else if (stdout) {
			System.out.println("JSON: " + writer.toString());
		}
	}

	public static void main(String... args) throws PatentReaderException, IOException {

		LOGGER.info("--- Start ---");

		OptionParser parser = new OptionParser() {
			{
				accepts("input").withRequiredArg().ofType(String.class).describedAs("Input File or Directory of Files")
						.required();
				accepts("limit").withOptionalArg().ofType(Integer.class).describedAs("total record limit")
						.defaultsTo(0);
				accepts("outdir").withOptionalArg().ofType(String.class).describedAs("output directory")
						.defaultsTo("output");
				accepts("outBulk").withOptionalArg().ofType(Boolean.class).describedAs("Single file record per line")
						.defaultsTo(true);
				accepts("flat").withOptionalArg().ofType(Boolean.class).describedAs("Flat json else hierarchy")
						.defaultsTo(false);
				accepts("prettyPrint").withOptionalArg().ofType(Boolean.class).describedAs("Pretty Print JSON")
						.defaultsTo(true);
				accepts("stdout").withOptionalArg().ofType(Boolean.class)
						.describedAs("Output to Terminal instead of File").defaultsTo(false);
				accepts("addHtmlEntities").withOptionalArg().ofType(Boolean.class)
						.describedAs("Add Html Entities DTD to XML; Needed when reading Patents in PAP format.")
						.defaultsTo(false);
			}
		};

		OptionSet options = parser.parse(args);
		if (!options.hasOptions()) {
			parser.printHelpOn(System.out);
			System.exit(1);
		}

		String inFileStr = (String) options.valueOf("input");
		Path inputPath = Paths.get(inFileStr);

		String outdir = (String) options.valueOf("outdir");
		Path outDirPath = Paths.get(outdir);

		int limit = (Integer) options.valueOf("limit");

		boolean flatJson = (Boolean) options.valueOf("flat");
		boolean prettyPrint = (Boolean) options.valueOf("prettyPrint");
		boolean stdout = (Boolean) options.valueOf("stdout");
		boolean addHtmlEntities = (Boolean) options.valueOf("addHtmlEntities");
		boolean outBulk = (Boolean) options.valueOf("outBulk");
		if (outBulk) {
			prettyPrint = false;
		}

		// ... Add Your Custom DocumentBuilder here ...

		DocumentBuilder<Patent> fileBuilder;
		if (flatJson) {
			fileBuilder = new JsonMapperFlat(prettyPrint, false);
		} else {
			fileBuilder = new JsonMapper(prettyPrint, false);
		}

		TransformerCli2 transform;
		if (stdout) {
			transform = new TransformerCli2(fileBuilder);
		} else {
			transform = new TransformerCli2(fileBuilder, outBulk);
		}

		if (addHtmlEntities) {
			transform.addHtmlEntities();
		}


		transform.process(inputPath, outDirPath);

		LOGGER.info("--- Done ---");
	}

}

