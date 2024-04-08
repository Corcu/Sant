package calypsox.tk.util;

import calypsox.ErrorCodeEnum;
import calypsox.tk.report.exception.SantExceptionType;
import calypsox.tk.report.exception.SantExceptions;
import com.calypso.tk.core.Log;
import com.calypso.tk.util.ScheduledTask;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * The Class SantScheduledTask.
 */
public abstract class SantScheduledTask extends ScheduledTask {

  /** The exceptions. */
  protected SantExceptions exceptions;

  /**
   * Instantiates a new sant scheduled task.
   */
  public SantScheduledTask() {
    exceptions = new SantExceptions();
  }

  private static SimpleDateFormat timeFormat = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());

  /**
   * Move input file.
   *
   * @param filename the filename
   * @param origFolder the orig folder
   * @param result the result
   */
  protected void moveInputFile(final String filename, final String origFolder, final boolean result) {
    final Date d = new Date();
    String time;
    synchronized (timeFormat) {
      time = timeFormat.format(d);
    }
    final String orig = origFolder + filename;
    String target = null;
    if (result) {
      target = origFolder + "ok" + File.separator + filename + "_" + time;
    } else {
      target = origFolder + "fail" + File.separator + filename + "_" + time;
    }
    try {
      MisFileUtility.moveFile(orig, target);
    } catch (final IOException e) {
      // FileUtility.moveFile
      Log.error(this, e.getMessage(), e);
    }
  }

  /**
   * Publish error.
   *
   * @param errorCode the error code
   * @param errorParams the error params
   * @param ex the ex
   * @param publishTasksNow the publish tasks now
   */
  protected void publishError(final ErrorCodeEnum errorCode, final String[] errorParams, final SantExceptionType ex,
                              final boolean publishTasksNow) {
    final String errorCodeDetail = errorCode.getFullTextMesssage(errorParams);
    //ControlMErrorLogger.addError(errorCode, errorCodeDetail);

    exceptions.addException(ex, getExternalReference(), errorCodeDetail, 0, 0, getId(),
        this.getClass().getSimpleName(), 0);
    Log.error(this, errorCodeDetail);
    if (publishTasksNow) {
      exceptions.publishTasks(getDSConnection(), 0, null);
    }
  }

  @Override
  protected List<AttributeDefinition> buildAttributeDefinition() {
    return new ArrayList<>();
  }

}
