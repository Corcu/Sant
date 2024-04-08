package calypsox.tk.anacredit.api.attributes;

import calypsox.tk.anacredit.api.ParseUtil;

public  class Alpha extends Attribute {
	public Alpha(int size) {
		super(DataType.ALPHA, size);
	}
	
	public String formatValue(Object valObj) {
		if (null != valObj) 	{
			return ParseUtil.formatStringWithBlankOnRight(valObj.toString(), getSize());
		}
		return ParseUtil.EMPTY_SPACE;
		
	}
	
}
