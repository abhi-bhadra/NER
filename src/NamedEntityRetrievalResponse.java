
public class NamedEntityRetrievalResponse {
	public NamedEntityRetrievalResponse() {
		super();
		// TODO Auto-generated constructor stub
	}

	public NamedEntityRetrievalResponse(int responseType) {
		super();
		this.responseType = responseType;
	}

	public static final int PERSON=1;
	public static final int LOCATION=2;
	public static final int OTHER=0;
	
	int responseType=0;
	String entity="";
	
	public static int getPerson() {
		return PERSON;
	}

	public static int getLocation() {
		return LOCATION;
	}

	public static int getOther() {
		return OTHER;
	}

	public boolean isPerson() {
		return responseType==getPerson();
	}
	
	public boolean isLocation() {
		return responseType==getLocation();
	}
	
	public boolean isOther() {
		return responseType==getOther();
	}

	public int getResponseType() {
		return responseType;
	}

	public void setResponseType(int responseType) {
		this.responseType = responseType;
	}

	@Override
	public String toString() {
		String result = "NER Response(" + entity + "): ";
		if (isPerson()) 
		result = result + "[PERSON]";
		if (isLocation()) 
		result = result + "[LOCATION]";
		if (isOther()) 
		result = result + "[OTHER]";
		
		return result;
	}

	public String getEntity() {
		return entity;
	}

	public void setEntity(String entity) {
		this.entity = entity;
	}
	

}
