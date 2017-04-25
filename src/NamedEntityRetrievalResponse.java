
public class NamedEntityRetrievalResponse {
	public NamedEntityRetrievalResponse() {
		super();
		responseType=0;
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
	
	public boolean isPerson() {
		return responseType==PERSON;
	}
	
	public boolean isLocation() {
		return responseType==LOCATION;
	}
	
	public boolean isOther() {
		return responseType==OTHER;
	}

	public int getResponseType() {
		return responseType;
	}

	public void setResponseType(int responseType) {
		this.responseType = responseType;
	}

	@Override
	public String toString() {
		String result = "NamedEntityRetrievalResponse ";
		if (isPerson()) 
		result = result + "[PERSON]";
		if (isLocation()) 
		result = result + "[LOCATION]";
		if (isOther()) 
		result = result + "[OTHER]";
		
		return result;
	}
	

}