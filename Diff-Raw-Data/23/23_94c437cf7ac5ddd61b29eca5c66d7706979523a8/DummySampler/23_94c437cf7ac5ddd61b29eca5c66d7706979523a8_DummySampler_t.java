 package kg.apc.jmeter.samplers;
 
 import org.apache.jmeter.samplers.AbstractSampler;
 import org.apache.jmeter.samplers.Entry;
 import org.apache.jmeter.samplers.SampleResult;
 
 /**
  *
  * @author apc
  */
 public class DummySampler
       extends AbstractSampler
 {
    /**
     *
     */
    public static final String IS_SUCCESSFUL = "SUCCESFULL";
    /**
     *
     */
    public static final String RESPONSE_CODE = "RESPONSE_CODE";
    /**
     *
     */
    public static final String RESPONSE_MESSAGE = "RESPONSE_MESSAGE";
    /**
     *
     */
    public static final String RESPONSE_DATA = "RESPONSE_DATA";
    /**
     *
     */
    public static final String RESPONSE_TIME = "RESPONSE_TIME";
 
    public SampleResult sample(Entry e)
    {
       SampleResult res = new SampleResult();
       res.setSampleLabel(getName());
 
       // source data
       res.setSamplerData(getResponseData());
 
       // response code
       res.setResponseCode(getResponseCode());
       res.setResponseMessage(getResponseMessage());
       res.setSuccessful(isSuccessfull());
 
       // responde data
       res.setDataType(SampleResult.TEXT);
       res.setResponseData(getResponseData().getBytes());
 
      // response time
      res.sampleStart();
      try
      {
         Thread.sleep(getResponseTime());
      }
      catch (InterruptedException ex)
      {
      }
      res.sampleEnd();

       return res;
    }
 
    /**
     *
     * @param selected
     */
    public void setSuccessful(boolean selected)
    {
       setProperty(IS_SUCCESSFUL, selected);
    }
 
    /**
     *
     * @param text
     */
    public void setResponseCode(String text)
    {
       setProperty(RESPONSE_CODE, text);
    }
 
    /**
     *
     * @param text
     */
    public void setResponseMessage(String text)
    {
       setProperty(RESPONSE_MESSAGE, text);
    }
 
    /**
     *
     * @param text
     */
    public void setResponseData(String text)
    {
       setProperty(RESPONSE_DATA, text);
    }
 
    /**
     * @return the successfull
     */
    public boolean isSuccessfull()
    {
       return getPropertyAsBoolean(IS_SUCCESSFUL);
    }
 
    /**
     * @return the responseCode
     */
    public String getResponseCode()
    {
       return getPropertyAsString(RESPONSE_CODE);
    }
 
    /**
     * @return the responseMessage
     */
    public String getResponseMessage()
    {
       return getPropertyAsString(RESPONSE_MESSAGE);
    }
 
    /**
     * @return the responseData
     */
    public String getResponseData()
    {
       return getPropertyAsString(RESPONSE_DATA);
    }
 
    /**
     *
     * @return
     */
    public int getResponseTime()
    {
       int time = 0;
       try
       {
          time = Integer.valueOf(getPropertyAsString(RESPONSE_TIME));
       }
       catch (NumberFormatException e)
       {
       }
       return time;
    }
 
    /**
     *
     * @param time
     */
    public void setResponseTime(String time)
    {
       setProperty(RESPONSE_TIME, time);
    }
 }
