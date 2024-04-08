/**
 * 
 */
package com.santander.collateral.util.email;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Defines an email message properties (from, to, subject, text, attachment files ...)
 * 
 * @author aela
 */
public class EmailMessage {

	private String from;
	private List<String> to;
	private List<String> toCc;
	private List<String> toBcc;
	private String subject;
	private String text;
	private String contentType; // html,
								// text...
	private List<Attachment> attachments = new ArrayList<EmailMessage.Attachment>();

	/**
	 * @return the contentType
	 */
	public String getContentType() {
		return this.contentType;
	}

	/**
	 * @param contentType
	 *            the contentType to set
	 */
	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	/**
	 * @return the from
	 */
	public String getFrom() {
		return this.from;
	}

	/**
	 * @param from
	 *            the from to set
	 */
	public void setFrom(String from) {
		this.from = from;
	}

	/**
	 * @return the to
	 */
	public List<String> getTo() {
		return this.to;
	}

	/**
	 * @param to
	 *            the to to set
	 */
	public void setTo(List<String> to) {
		this.to = to;
	}

	/**
	 * @return the subject
	 */
	public String getSubject() {
		return this.subject;
	}

	/**
	 * @param subject
	 *            the subject to set
	 */
	public void setSubject(String subject) {
		this.subject = subject;
	}

	/**
	 * @return the text
	 */
	public String getText() {
		return this.text;
	}

	/**
	 * @param text
	 *            the text to set
	 */
	public void setText(String text) {
		this.text = text;
	}

	/**
	 * @return the attachments
	 */
	public List<Attachment> getAttachments() {
		return this.attachments;
	}

	/**
	 * @param attachments
	 *            the attachments to set
	 */
	public void setAttachments(List<Attachment> attachments) {
		this.attachments = attachments;
	}

	/**
	 * Add an attachment with the given information
	 * 
	 * @param mimeType
	 * @param attachmentName
	 * @param content
	 */
	public void addAttachment(String mimeType, String attachmentName, Object content) {
		if (this.attachments == null) {
			this.attachments = new ArrayList<EmailMessage.Attachment>();
		}
		this.attachments.add(new Attachment(mimeType, attachmentName, content));
	}

	/**
	 * Rest the list of attachments for this email
	 */
	public void resetAttachmentList() {
		this.attachments = new ArrayList<EmailMessage.Attachment>();
	}

	/**
	 * Add the list of files as attachments to the current email
	 * 
	 * @param fileAbsolutePaths
	 *            list of file paths
	 * @throws IOException
	 */
	public void addAttachment(List<String> fileAbsolutePaths) throws IOException {
		if (this.attachments == null) {
			this.attachments = new ArrayList<EmailMessage.Attachment>();
		}
		for (String filePath : fileAbsolutePaths) {
			// get the file from the path
			File file = new File(filePath);
			// // get the file mimetype
			// String mimeType =
			// FILE_NAME_MAP.getContentTypeFor(file.getName());
			// // get the file content
			// FileInputStream fis = new FileInputStream(file);
			// byte[] fileContent = new byte[fis.available()];
			// fis.read(fileContent);
			// attachments.add(new Attachment(mimeType, file.getName(),
			// fileContent));
			this.attachments.add(new Attachment(file));

		}
	}

	/**
	 * Define the structure of an email attachment
	 * 
	 * @author aela
	 */
	class Attachment {

		protected String attachmentName;
		protected Object content;
		protected String mimeType;
		protected File file;

		/**
		 * @param mimeType
		 * @param attachmentName
		 * @param content
		 */
		public Attachment(String mimeType, String attachmentName, Object content) {
			this.mimeType = mimeType;
			this.content = content;
			this.attachmentName = attachmentName;
		}

		/**
		 * @param file
		 *            the file content to add as an attachment
		 */
		public Attachment(File file) {
			this.file = file;
			this.attachmentName = file.getName();
		}

		/**
		 * @return the mimeType
		 */
		public String getMimeType() {
			return this.mimeType;
		}

		/**
		 * @param mimeType
		 *            the mimeType to set
		 */
		public void setMimeType(String mimeType) {
			this.mimeType = mimeType;
		}

		/**
		 * @return the attachmentName
		 */
		public String getAttachmentName() {
			return this.attachmentName;
		}

		/**
		 * @param attachmentName
		 *            the attachmentName to set
		 */
		public void setAttachmentName(String attachmentName) {
			this.attachmentName = attachmentName;
		}

		/**
		 * @return the content
		 */
		public Object getContent() {
			return this.content;
		}

		/**
		 * @param content
		 *            the content to set
		 */
		public void setContent(Object content) {
			this.content = content;
		}

		public File getFile() {
			return this.file;
		}

		public void setFile(File file) {
			this.file = file;
		}

	}

	/**
	 * @return the toBcc
	 */
	public List<String> getToBcc() {
		return this.toBcc;
	}

	/**
	 * @param toBcc
	 *            the toBcc to set
	 */
	public void setToBcc(List<String> toBcc) {
		this.toBcc = toBcc;
	}

	/**
	 * @return the toCc
	 */
	public List<String> getToCc() {
		return this.toCc;
	}

	/**
	 * @param toCc
	 *            the toCc to set
	 */
	public void setToCc(List<String> toCc) {
		this.toCc = toCc;
	}

}
