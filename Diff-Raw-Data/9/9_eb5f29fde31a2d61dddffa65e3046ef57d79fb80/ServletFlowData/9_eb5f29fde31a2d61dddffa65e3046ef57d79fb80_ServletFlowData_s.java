 package wint.mvc.flow;
 
 import java.io.IOException;
 import java.io.OutputStream;
 import java.io.Writer;
 import java.util.Locale;
 import java.util.Map;
 
 import javax.servlet.http.HttpServletRequest;
 import javax.servlet.http.HttpServletResponse;
 import javax.servlet.http.HttpSession;
 
 import wint.core.config.Constants;
 import wint.core.service.ServiceContext;
 import wint.lang.WintException;
 import wint.lang.exceptions.FlowDataException;
 import wint.lang.io.FastByteArrayOutputStream;
 import wint.lang.io.FastStringWriter;
 import wint.lang.misc.profiler.Profiler;
 import wint.lang.utils.IoUtil;
 import wint.lang.utils.MapUtil;
 import wint.lang.utils.StringUtil;
 import wint.mvc.form.Form;
 import wint.mvc.form.FormService;
 import wint.mvc.form.fileupload.UploadFile;
 import wint.mvc.module.Module;
 import wint.mvc.parameters.Arguments;
 import wint.mvc.parameters.Parameters;
 import wint.mvc.parameters.ServletParameters;
 import wint.mvc.servlet.ServletUtil;
 import wint.mvc.template.Context;
 import wint.mvc.url.UrlBroker;
 import wint.mvc.url.UrlBrokerService;
 import wint.mvc.url.UrlModule;
 import wint.mvc.view.Render;
 import wint.mvc.view.StringRender;
 import wint.mvc.view.types.NopViewRender;
 import wint.mvc.view.types.TemplateViewRender;
 import wint.mvc.view.types.ViewTypes;
 
 /**
  * @author pister
  * 2012-1-11 02:37:51
  */
 public class ServletFlowData implements InnerFlowData {
 
 	protected Map<String, Object> attributes = MapUtil.newHashMap();
 	
 	private HttpServletResponse httpServletResponse;
 	
 	private HttpServletRequest httpServletRequest;
 	
 	private ServiceContext serviceContext;
 	
 	private ServletParameters servletParameters;
 	
 	private String target;
 	
 	private int statusCode = HttpServletResponse.SC_OK;
 
 	private boolean redirected = false;
 	
 	private boolean sendRedirected = false;
 	
 	private boolean commited = false;
 	
 	private FastStringWriter fastStringWriter;
 	
 	private FastByteArrayOutputStream fastByteArrayOutputStream;
 	
 	private String contentType;
 	
 	private Locale locale;
 	
 	private Context innerContext;
 	
 	private Context context;
 	
 	private Arguments arguments;
 	
 	private Render sendToLocation;
 	
 	private String viewType;
 	
 	private String layout;
 	
 	private Module module;
 	
 	public ServletFlowData(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, ServiceContext serviceContext) {
 		super();
 		this.httpServletRequest = httpServletRequest;
 		this.httpServletResponse = httpServletResponse;
 		this.serviceContext = serviceContext;
 		
 		servletParameters = new ServletParameters(httpServletRequest);
 		target = ServletUtil.getServletPath(httpServletRequest);
 		locale = httpServletRequest.getLocale();
 		
 		viewType = ViewTypes.TEMPLATE_VIEW_TYPE;
 	}
 
 	public Form getForm(String name) {
 		FormService formService = this.serviceContext.getService(FormService.class);
 		Form form = formService.getForm(name, this);
 		return form;
 	}
 
 	protected String getDefaultContentType() {
 		String charsetEncoding = serviceContext.getConfiguration().getProperties().getString(Constants.PropertyKeys.CHARSET_ENCODING, Constants.Defaults.CHARSET_ENCODING);
 		return "text/html; charset=" + charsetEncoding;
 	}
 	
 	public void resetRedirected() {
 		sendRedirected = false;
 		redirected = false;
 	}
 
 	public void redirectTo(String location) {
 		this.sendToLocation = new StringRender(location);
 		sendRedirected = true;
 	}
 	
 	public void sendRedirectPermanently(String location) {
 		sendRedirected = true;
 		// 使用301代替302的目标是为了seo优化
 		httpServletResponse.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);	// 301
 		httpServletResponse.setHeader("Location", location);
 	}
 	
 	public Object getAttribute(String name) {
 		return attributes.get(name);
 	}
 
 	public void setAttribute(String name, Object value) {
 		attributes.put(name, value);
 	}
 
 	public Parameters getParameters() {
 		return servletParameters;
 	}
 
 	public String getTarget() {
 		return target;
 	}
 
 	public void setTarget(String target) {
 		this.target = target;
 	}
 
 	public void forwardTo(String target) {
 		this.target = target;
 		redirected = true;
 	}
 
 	public void setViewType(String viewType) {
 		this.viewType = viewType;
 	}
 
 	public String getViewType() {
 		return viewType;
 	}
 
 	public boolean isSendRedirected() {
 		return sendRedirected;
 	}
 	
 	public boolean isForwardTo() {
 		return redirected;
 	}
 
 	public Writer getWriter() {
 		setViewType(ViewTypes.NOP_VIEW_TYPE);
 		if (fastStringWriter != null) {
 			return fastStringWriter;
 		}
 		fastStringWriter = new FastStringWriter();
 		return fastStringWriter;
 	}
 
 	public OutputStream getOutputStream() {
 		setViewType(ViewTypes.NOP_VIEW_TYPE);
 		if (fastByteArrayOutputStream != null) {
 			return fastByteArrayOutputStream;
 		}
 		fastByteArrayOutputStream = new FastByteArrayOutputStream();
 		return fastByteArrayOutputStream;
 	}
 	
 	public UrlBroker redirectTo(String urlModuleName, String target) {
 		UrlBrokerService urlBrokerService = serviceContext.getService(UrlBrokerService.class);
 		UrlModule urModule = urlBrokerService.getUrlModules().get(urlModuleName);
 		if (urModule == null) {
 			return null;
 		}
 		UrlBroker urlBroker = urModule.setTarget(target);
 		this.sendToLocation = urlBroker;
 		this.sendRedirected = true;
 		return urlBroker;
 	}
 	
 	public UrlBroker forkUrl(String urlModuleName, String target) {
 		UrlBrokerService urlBrokerService = serviceContext.getService(UrlBrokerService.class);
 		UrlModule urModule = urlBrokerService.getUrlModules().get(urlModuleName);
 		UrlBroker urlBroker = urModule.setTarget(target);
 		return urlBroker;
 	}
 
 	public void commitData() {
 		try {
 			Profiler.enter("committing data.");
 			if (commited) {
 				throw new FlowDataException("flowData has been commited!");
 			}
 			if (sendRedirected) {
 				String redirectUrl = sendToLocation.render();
 				try {
 					this.httpServletResponse.sendRedirect(redirectUrl);
 				} catch (IOException e) {
 					throw new FlowDataException(e);
 				}
 				return;
 			}
 			if (fastByteArrayOutputStream != null && fastStringWriter != null) {
 				throw new FlowDataException("please do not write on getOuputStream() and getWriter() both");
 			}
 			
 			if (!StringUtil.isEmpty(contentType)) {
 				httpServletResponse.setContentType(contentType);
 			} else {
 				httpServletResponse.setContentType(getDefaultContentType());
 			}
 			if (fastStringWriter != null) {
 				Writer out = null;
 				try {
 					out = httpServletResponse.getWriter();
 					out.write(fastStringWriter.toString());
 				} catch (IOException e) {
 					throw new FlowDataException(e);
 				} finally {
 					IoUtil.close(out);
 				}
 			} else if (fastByteArrayOutputStream != null) {
 				OutputStream os = null;
 				try {
 					os = httpServletResponse.getOutputStream();
 					os.write(fastByteArrayOutputStream.toByteArray());
 				} catch (IOException e) {
 					throw new FlowDataException(e);
 				} finally {
 					IoUtil.close(os);
 				}
 			}
 			commited = true;
 		}  finally {
 			Profiler.release();
 		}
 	}
 
 	public void setStatusCode(int code) {
 		httpServletResponse.setStatus(code);
 		this.statusCode = code;
 	}
 
 	public int getStatusCode() {
 		return statusCode;
 	}
 
 	public String getContentType() {
 		return contentType;
 	}
 
 	public void setContentType(String contentType) {
 		this.contentType = contentType;
 	}
 
 	public Locale getLocale() {
 		return locale;
 	}
 
 	public void setLocale(Locale locale) {
 		this.locale = locale;
 	}
 
 	public Arguments getArguments() {
 		return arguments;
 	}
 
 	public void setArguments(Arguments arguments) {
 		this.arguments = arguments;
 	}
 
 	public Context getInnerContext() {
 		return innerContext;
 	}
 
 	public void setInnerContext(Context innerContext) {
 		this.innerContext = innerContext;
 	}
 
 	public ServiceContext getServiceContext() {
 		return serviceContext;
 	}
 	
 	public Session getSession() {
 		return new ServletHttpSession(httpServletRequest.getSession());
 	}
 	
 	public HttpServletRequest getRequest() {
 		return httpServletRequest;
 	}
 	
 	public HttpServletResponse getResponse() {
 		return httpServletResponse;
 	}
 
 	public Context getContext() {
 		return context;
 	}
 
 	public void setContext(Context context) {
 		this.context = context;
 	}
 
 	public Map<String, UploadFile> getUploadFiles() {
 		throw new WintException("Unsupport operation on normal flowdata: \r\n" +
 				"Please add the property  enctype=\"multipart/form-data\"  to your form, \r\n" +
 				"Add commons-uploadfile library to your classpath (optional: commons-io).\r\n" +
 				"a suggest maven dependencies are:\r\n\r\n" +
 				"<dependency>\r\n\t<groupId>commons-fileupload</groupId>\r\n\t<artifactId>commons-fileupload</artifactId>\r\n\t<version>1.2.2</version>\r\n</dependency>\r\n\r\n" +
 				"<dependency>\r\n\t<groupId>commons-io</groupId>\r\n\t<artifactId>commons-io</artifactId>\r\n\t<version>2.1</version>\r\n</dependency>\r\n\r\n");
 	}
 
 	public String getLayout() {
 		return layout;
 	}
 
 	public void setLayout(String layout) {
 		this.layout = layout;
 	}
 
 	public Module getModule() {
 		return module;
 	}
 
 	public void setModule(Module module) {
 		this.module = module;
 	}
 
    public Arguments getPathContextArguments() {
        // TODO
        return null;
    }

    public Parameters getPathContextPetermeters() {
        // TODO
        return null;
    }
 }
