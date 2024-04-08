package com.calypso.tk.report;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Vector;

import org.jfree.util.Log;

import com.calypso.tk.core.Amount;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.Util;
import com.calypso.tk.util.InstantiateUtil;

/**
 * 
 * @author x660030 Viewer to get data of the report and format the fields into a
 *         bean object or a generic Hashmap object with the information. It can
 *         be used to create a JSON in a WebService, for example.
 */
public class JavaBeanReportViewer extends AbstractReportViewer {
	public static final String BEAN_CLASS_LABEL = "BEAN_CLASS";
	public static final String SUB_BEAN_CLASS_LABEL = "SUB_BEAN_CLASS";
	public static final String SUB_BEAN_FIELD_NAME = "SUB_BEAN_FIELD_NAME";
	private ArrayList<Object> beanList = null;
	private ArrayList<Object> subRowList = null;
	private String strBeanClassName = "";
	private String strSubBeanClassName = "";
	private String strSubBeanFieldName = "";
	private Object rowBean = null;

	/**
	 * @param output    Output of the report.
	 * @param template  Template of the report. It should contain the BEAN_CLASS
	 *                  property if you want to use it.
	 * @param view      See AbstractReportViewer.
	 * @param forceInit See AbstractReportViewer. Initialize the bean class
	 *                  variables in order to be used in the viewer and superclass
	 *                  viewer.
	 */
	@Override
	public void init(DefaultReportOutput output, ReportTemplate template, ReportView view, boolean forceInit) {
		super.init(output, template, view, forceInit);
		strBeanClassName = template.get(BEAN_CLASS_LABEL);
		strSubBeanClassName = template.get(SUB_BEAN_CLASS_LABEL);
		strSubBeanFieldName = template.get(SUB_BEAN_FIELD_NAME);
		if (Util.isEmpty(strBeanClassName)) {
			Log.info("Could not get a java bean class name. Using a HashMap.");
		}
		if (Util.isEmpty(strSubBeanClassName)) {
			Log.info("Could not get a java sub bean class name. Using a HashMap.");
		}
		if (Util.isEmpty(strSubBeanFieldName)) {
			strSubBeanFieldName = "operations";
			Log.info("Could not get a java sub bean class name. Using a default field name: operations.");
		}
	}

	/**
	 * 
	 * @param name  Field name to be formatted.
	 * @param type  type of Row.
	 * @param value Value of the field to be formatted.
	 */
	protected void formatCell(String name, int type, Object value) {
		if ((type == 4 && this.isInColumnMap(this.subheadingMap, name))
				|| (type == 7 && !this.isInColumnMap(this.subheadingMap, name))) {
			transformToObject(name, value);
		}
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void setSubheading(ReportRow row, Object[] subheadings, Vector aggregations) {
		boolean aggregateReport = this.template.useAggregation();
		if (aggregateReport) {
			this.aggregationRootNode.getNode(aggregations);
		} else {
			int columnAt = 0;
			if (this.template.getSubheadings() == null || this.template.getSubheadings().length == 0) {
				return;
			}
			this.startRow(4);
			for (int i = 0; i < this.columns.length; ++i) {
				String column = this.columns[i];
				if (!this.isInColumnMap(this.subheadingMap, column)) {

					if (this.displaySubheadingAsColumn()) {
						this.formatCell(columnAt++, 4, (Object) null);
					}
				} else {
					this.formatCell(columnAt++, 4, subheadings[i]);
				}
			}
			this.endRow(4);
			this.emptyReport = false;
		}
	}

	private void transformToObject(String name, Object value) {
		try {
			if (value != null && !Util.isEmpty(name) && !Util.isEmpty(strBeanClassName)) {
				Field field;
				field = Class.forName(strBeanClassName).getDeclaredField(name);
				Method method = Class.forName(strBeanClassName).getMethod("set" + Util.capitalize(name),
						field.getType());
				if (value instanceof JDate) {
					Date date = ((JDate) value).getDate();
					setMethodInvocation(field, method, date);
				} else if (value instanceof JDatetime) {
					Date date = (JDatetime) value;
					setMethodInvocation(field, method, date);
				} else if (value instanceof Amount) {
					method.invoke(rowBean, field.getType().cast(((Amount) value).get()));
				} else {
					method.invoke(rowBean, field.getType().cast(value));
				}
			} else {
				if (rowBean instanceof LinkedHashMap<?, ?>) {
					@SuppressWarnings("unchecked")
					LinkedHashMap<String, Object> hashBean = (LinkedHashMap<String, Object>) rowBean;
					if (value instanceof JDate) {
						Date dat = ((JDate) value).getDate();
						LocalDate lDate = dat.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
						hashBean.put(name, lDate);
					} else if (value instanceof JDatetime) {
						Date dat = ((JDatetime) value);
						LocalDateTime lDate = dat.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
						hashBean.put(name, lDate);
					} else if (value instanceof Amount) {
						int dig=((Amount)value).getDigit();
						if (dig < 0) {
							dig=13;
						}
						BigDecimal bd = null;
						if (!Double.isNaN(((Amount) value).get())) {
							bd = BigDecimal.valueOf(((Amount) value).get()).setScale(dig, BigDecimal.ROUND_HALF_UP);
						}
						if (bd != null && ((Amount) value).getDigit() < 0) {
							bd=bd.stripTrailingZeros();
						}
						hashBean.put(name,bd);
					} else {
						hashBean.put(name, value);
					}
				}
			}
		} catch (NoSuchFieldException e) {
			Log.error("Cannot set value for field " + name + " in bean class. Field doesn´t exists.", e);
		} catch (NoSuchMethodException e) {
			Log.error("Cannot set value for field " + name + " in bean class. Setter method " + "set"
					+ Util.capitalize(name) + " does not exists.", e);
		} catch (SecurityException | ClassNotFoundException | IllegalAccessException | IllegalArgumentException
				| InvocationTargetException e) {
			Log.error("Error getting methods or fields for java bean class name.", e);
			throw new RuntimeException("Error getting methods or fields for java bean class name.", e);
		} catch (ClassCastException e) {
			Log.error("Error casting report data to bean field.", e);
			throw e;
		}
	}

	private void setMethodInvocation(Field field, Method method, Date date)
			throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		if (field.getType().equals(Date.class)) {
			method.invoke(rowBean, date);
		} else if (field.getType().equals(LocalDate.class)) {
			LocalDate lDate = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
			method.invoke(rowBean, lDate);
		} else if (field.getType().equals(String.class)) {
			method.invoke(rowBean, date.toString());
		} else if (field.getType().equals(Long.class)) {
			Long longNum = date.getTime();
			method.invoke(rowBean, longNum);
		}
	}

	/**
	 * 
	 * @param columnIndex Index of the field to be formatted.
	 * @param type        type of Row.
	 * @param value       Value of the field to be formatted.
	 */
	@Override
	protected void formatCell(int columnIndex, int type, Object value) {
		formatCell(columns[columnIndex], type, value);
	}

	/**
	 * @param type Type of the Row. It create a new object of the bean to be
	 *             completed in formatRow method.
	 */
	@Override
	protected void startRow(int type) {
		if (!Util.isEmpty(this.subheadingMap)) {
			if (type == 4) {
				subRowList = new ArrayList<>();

				if (!Util.isEmpty(strBeanClassName)) {
					try {
						rowBean = InstantiateUtil.getInstance(strBeanClassName);
					} catch (InstantiationException | IllegalAccessException e) {
						Log.error("Error getting java bean class name.", e);
						throw new RuntimeException(e);
					}
				} else {
					rowBean = new LinkedHashMap<String, Object>();

				}
			} else if (type == 7) {
				if (!Util.isEmpty(strSubBeanClassName)) {
					try {
						rowBean = InstantiateUtil.getInstance(strSubBeanClassName);
					} catch (InstantiationException | IllegalAccessException e) {
						Log.error("Error getting java sub bean class name.", e);
						throw new RuntimeException(e);
					}
				} else {
					rowBean = new LinkedHashMap<String, Object>();
				}
			}
		} else {
			if (type == 7) {
				if (!Util.isEmpty(strBeanClassName)) {
					try {
						rowBean = InstantiateUtil.getInstance(strBeanClassName);
					} catch (InstantiationException | IllegalAccessException e) {
						Log.error("Error getting java bean class name.", e);
						throw new RuntimeException(e);
					}
				} else {
					rowBean = new LinkedHashMap<String, Object>();

				}
			}
		}
	}

	/**
	 * @param type Type of the Row. It adds the current object to the beanList
	 *             array.
	 */
	@Override
	protected void endRow(int type) {
		if (!Util.isEmpty(this.subheadingMap)) {
			if (type == 4) {
				if (beanList == null) {
					beanList = new ArrayList<>();
				}
				if (subRowList == null) {
					subRowList = new ArrayList<>();
				}
				transformToObject(strSubBeanFieldName, subRowList);
				beanList.add(rowBean);

			} else if (type == 7) {
				if (subRowList == null) {
					subRowList = new ArrayList<>();
				}
				if (!Util.isEmpty(subRowList) || !checkEmptySubRow(rowBean)) {
					subRowList.add(rowBean);
				}
			}
		} else {
			if (type == 7) {
				if (beanList == null) {
					beanList = new ArrayList<>();
				}
				beanList.add(rowBean);
			}

		}
	}

	private boolean checkEmptySubRow(Object rowBean) {
		if (rowBean instanceof LinkedHashMap<?, ?>) {
			LinkedHashMap<?, ?> row = (LinkedHashMap<?, ?>) rowBean;
			Collection<?> vals = row.values();
			for (Object object : vals) {
				if (object != null) {
					return false;
				}
			}
		} else {
			try {
				Field[] fields = Class.forName(strBeanClassName).getDeclaredFields();
				for (Field field : fields) {
					Method method = Class.forName(strBeanClassName).getMethod("get" + Util.capitalize(field.getName()),
							field.getType());
					Object value = method.invoke(rowBean);
					if (value != null) {
						return false;
					}
				}
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | SecurityException
					| ClassNotFoundException | NoSuchMethodException e) {
				return true;
			}
		}
		return true;
	}

	/**
	 * This method is not supported.
	 * 
	 * @param paramInt
	 * @param paramObject
	 */
	@Override
	void formatCell(int paramInt, Object paramObject) {
		Log.error("Error formatCell method not used.");
		throw new RuntimeException("Error formatCell method not used.");
	}

	/**
	 * This method returns the list of beans generated.
	 * 
	 * @return List of objects with information of the report.
	 */
	public List<Object> getBeanList() {
		return beanList;
	}
}
