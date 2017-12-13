package gov.uspto.bulkdata.cli2;

import java.net.MalformedURLException;

public enum BulkDataType {

	//03/15/2017 NOTE:  The disk space on BDSS is being restructured. URLs/links that contain data2 and data3 will be changing to data.
	GAZETTE("data/patent/officialgazette/", "zip", 2002),

	//GRANT_MULTI_PAGE_IMAGES("data/patent/grant/multipagetiff/", "zip"),
	//GRANT_SINGLE_PAGE_IMAGES("data/patent/grant/yellowbook/", "tar"),
	//GRANT_REDBOOK_WITH_IMAGES("grant/redbook/", "tar"),
	GRANT_REDBOOK_TEXT("data/patent/grant/redbook/fulltext/", "zip", 1976),
	//GRANT_REDBOOK_BIBLIO("data/patent/grant/redbook/bibliographic/", "zip"),

	//APPLICATION_MULTI_PAGE_IMAGES("data/patent/application/multipagetiff/", "zip"),
	//APPLICATION_SINGLE_PAGE_IMAGES("data/patent/application/yellowbook/", "tar"),
	//APPLICATION_REDBOOK_WITH_IMAGES("data/patent/application/redbook/", "tar"),
	APPLICATION_REDBOOK_TEXT("data/patent/application/redbook/fulltext/", "zip", 2001);
	//APPLICATION_REDBOOK_BIBLIO("data/patent/grant/redbook/bibliographic/", "zip");

	private static String BASEURL = "https://bulkdata.uspto.gov/";
	private String restPath;
	private String suffix;
	private Integer startAvailableYear;

	BulkDataType(String restPath, String suffix, Integer startAvailableYear){
		this.restPath = restPath;
		this.suffix = suffix;
		this.startAvailableYear = startAvailableYear;
	}

	public String getRestPath(){
		return restPath;
	}

	public String getSuffix(){
		return suffix;
	}

	public Integer getStartAvailableYear() {
		return startAvailableYear;
	}

	public String getURL(String year) throws MalformedURLException{
		StringBuilder stb = new StringBuilder();
		return stb.append(BASEURL).append(this.restPath).append(year).append("/").toString();
	}

	public String getURL(Integer year) throws MalformedURLException{
		return this.getURL(String.valueOf(year));
	}
}
