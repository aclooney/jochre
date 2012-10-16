package com.joliciel.jochre.web;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Locale;
import java.util.ArrayList;
import java.util.Properties;


import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.zkoss.util.media.Media;
import org.zkoss.util.resource.Labels;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Session;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.ForwardEvent;
import org.zkoss.zk.ui.event.UploadEvent;
import org.zkoss.zk.ui.util.GenericForwardComposer;
import org.zkoss.zkplus.databind.AnnotateDataBinder;
import org.zkoss.zul.Button;
import org.zkoss.zul.Div;
import org.zkoss.zul.Fileupload;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Groupbox;
import org.zkoss.zul.Hlayout;
import org.zkoss.zul.Html;
import org.zkoss.zul.Label;
import org.zkoss.zul.Panel;
import org.zkoss.zul.Progressmeter;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Timer;
import org.zkoss.zul.Window;

import com.joliciel.jochre.JochreServiceLocator;
import com.joliciel.jochre.doc.DocumentService;
import com.joliciel.jochre.doc.ImageDocumentExtractor;
import com.joliciel.jochre.doc.JochreDocument;
import com.joliciel.jochre.doc.JochreDocumentGenerator;
import com.joliciel.jochre.doc.JochrePage;
import com.joliciel.jochre.doc.ProcessedImageObserver;
import com.joliciel.jochre.graphics.GraphicsService;
import com.joliciel.jochre.graphics.JochreImage;
import com.joliciel.jochre.lexicon.Lexicon;
import com.joliciel.jochre.lexicon.LexiconService;
import com.joliciel.jochre.lexicon.LocaleSpecificLexiconService;
import com.joliciel.jochre.lexicon.MostLikelyWordChooser;
import com.joliciel.jochre.lexicon.WordSplitter;
import com.joliciel.jochre.pdf.PdfImageVisitor;
import com.joliciel.jochre.pdf.PdfService;
import com.joliciel.jochre.security.User;
import com.joliciel.jochre.text.TextFormat;
import com.joliciel.jochre.text.TextGetter;
import com.joliciel.jochre.text.TextService;

import com.joliciel.talismane.utils.LogUtils;
import com.joliciel.talismane.utils.MessageResource;
import com.joliciel.talismane.utils.ProgressMonitor;

public class TextController extends GenericForwardComposer<Window> {
	private static final long serialVersionUID = 5620794383603025597L;

	private static final Log LOG = LogFactory.getLog(TextController.class);

	private JochreServiceLocator locator = null;
	private GraphicsService graphicsService;
	private DocumentService documentService;
	private LexiconService lexiconService;
	private TextService textService;
	
	private JochreDocument currentDoc;
	private JochreImage currentImage;
	private User currentUser;
	private File currentFile;
	private JochreDocumentGenerator documentGenerator;
	private DocumentHtmlGenerator documentHtmlGenerator;
	private ProgressMonitor progressMonitor;
	
	private Properties jochreProperties;
	
	private int currentHtmlIndex = 0;
	
	AnnotateDataBinder binder;

	Window winJochreText;
	Grid documentGrid;
	Grid gridPages;
	Label lblDocName;
	Div htmlContent;
	Timer startRenderTimer;
	Progressmeter progressMeter1;
	Hlayout progressBox;
	Panel uploadPanel;
	Fileupload fileUpload1;
	Textbox txtStartPage;
	Textbox txtEndPage;
	Button btnAnalyse;
	Button btnDone;
	Button btnUpload;
	Label lblAwaitingFile;
	Label lblFileName;
	Label lblCurrentAction;
	Timer progressTimer;
	Groupbox errorBox;
	Label lblErrorMessage;

	int currentPageIndex = 0;

	public TextController() {
	}

	public void doAfterCompose(Window window) throws Exception {
		try {
			super.doAfterCompose(window);
			Session session = Sessions.getCurrent();
			currentUser = (User) session.getAttribute(LoginController.SESSION_JOCHRE_USER);
			if (currentUser==null)
				Executions.sendRedirect("login.zul");

			locator = JochreServiceLocator.getInstance();

			String resourcePath = "/jdbc-jochreWeb.properties";
			LOG.debug("resource path: " + resourcePath);
			locator.setDataSourceProperties(this.getClass().getResourceAsStream(resourcePath));
			graphicsService = locator.getGraphicsServiceLocator().getGraphicsService();
			textService = locator.getTextServiceLocator().getTextService();
			documentService = locator.getDocumentServiceLocator().getDocumentService();
			lexiconService = locator.getLexiconServiceLocator().getLexiconService();

			String jochrePropertiesPath = "/jochre.properties";
			jochreProperties = new Properties();
			jochreProperties.load(this.getClass().getResourceAsStream(jochrePropertiesPath));
			
			HttpServletRequest request = (HttpServletRequest) Executions.getCurrent().getNativeRequest();
			if (request.getParameter("imageId")!=null) {
				int imageId = Integer.parseInt(request.getParameter("imageId"));
				currentImage = graphicsService.loadJochreImage(imageId);
				currentDoc = currentImage.getPage().getDocument();
				uploadPanel.setVisible(false);
				progressBox.setVisible(true);
				startRenderTimer.setRunning(true);
			} else if (request.getParameter("docId")!=null) {
				int docId = Integer.parseInt(request.getParameter("docId"));
				currentDoc = documentService.loadJochreDocument(docId);
				if (request.getParameter("addPages")!=null)
				{
					uploadPanel.setVisible(true);
					//progressBox.setVisible(false);
					lblAwaitingFile.setVisible(true);
				} else {
					uploadPanel.setVisible(false);
					progressBox.setVisible(true);
					startRenderTimer.setRunning(true);
				}
			} else {
				uploadPanel.setVisible(true);
				//progressBox.setVisible(false);
				lblAwaitingFile.setVisible(true);
			}

			if (currentDoc!=null&&!currentDoc.isLeftToRight())
				htmlContent.setSclass("rightToLeft");
			
			if (currentDoc!=null) {
				documentGrid.setVisible(true);
				lblDocName.setValue(currentDoc.getName());
			}

			binder = new AnnotateDataBinder(window);
			binder.loadAll();
		} catch (Exception e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		}
	}

	public void onTimer$startRenderTimer(Event event) {
		try {
			progressBox.setVisible(true);
			TextGetter textGetter = textService.getTextGetter();
			if (currentImage!=null) {
				Html html = new Html();
				StringWriter out = new StringWriter();

				textGetter.getText(currentImage, out, TextFormat.XHTML);
				html.setContent(out.toString());
				htmlContent.appendChild(html);
				progressMeter1.setValue(100);
				//progressBox.setVisible(false);
			} else {
				if (currentPageIndex<currentDoc.getPages().size()) {
					JochrePage page = currentDoc.getPages().get(currentPageIndex);
					for (JochreImage image : page.getImages()) {
						Html html = new Html();
						StringWriter out = new StringWriter();
						textGetter.getText(image, out, TextFormat.XHTML);
						out.append("<HR/>");
						html.setContent(out.toString());
						htmlContent.appendChild(html);
					}
					page.clearMemory();
					currentPageIndex++;
					double percentComplete = ((double)currentPageIndex / (double)currentDoc.getPages().size()) * 100;
					progressMeter1.setValue(new Double(percentComplete).intValue());
					startRenderTimer.setRunning(true);
				} else {
					progressMeter1.setValue(100);
					//progressBox.setVisible(false);
				}
			}
		} catch (Exception e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		}

	}

	public void onUpload$btnUpload(Event event) {
		try {
			LOG.debug("onUpload$btnUpload");
			
			ForwardEvent forwardEvent = (ForwardEvent) event;
			UploadEvent uploadEvent = (UploadEvent) forwardEvent.getOrigin();

			Media media = uploadEvent.getMedia();
			// save this to the temp file location.
			ServletContext servletContext = (ServletContext) Executions.getCurrent().getDesktop().getWebApp().getServletContext();
			File tempDir = (File) servletContext.getAttribute("javax.servlet.context.tempdir");
			LOG.debug("Temp dir: " + tempDir.getPath());
			currentFile = new File(tempDir, media.getName());
			LOG.debug("Filename: " + media.getName());
			FileOutputStream out = new FileOutputStream(currentFile);
			InputStream in = media.getStreamData();
		    byte buf[]=new byte[1024];
		    int len;
		    while((len=in.read(buf))>0)
		    	out.write(buf,0,len);
		    out.close();
		    in.close();
		    lblFileName.setValue(media.getName());
			btnAnalyse.setDisabled(false);
			btnUpload.setDisabled(true);
			if (media.getName().endsWith(".pdf"))
				gridPages.setVisible(true);
		} catch (Exception e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		}
	}
	
	public void onClick$btnAnalyse(Event event) {
		try {
			LOG.debug("onClick$btnAnalyse");
			if (currentFile!=null) {
				TextGetter textGetter = textService.getTextGetter();
				progressBox.setVisible(true);
				lblAwaitingFile.setVisible(false);
				int startPage = txtStartPage.getValue().length()==0 ? -1 : Integer.parseInt(txtStartPage.getValue());
				int endPage = txtEndPage.getValue().length()==0 ? -1 : Integer.parseInt(txtEndPage.getValue());
				
				ServletContext servletContext = (ServletContext) Executions.getCurrent().getDesktop().getWebApp().getServletContext();
				String letterModelPath = jochreProperties.getProperty("letterModelPath");
				LOG.debug("letterModelPath: " + letterModelPath);
				String letterModelRealPath = servletContext.getRealPath(letterModelPath);
				File letterModelFile = new File(letterModelRealPath);

				String splitModelPath = jochreProperties.getProperty("splitModelPath");
				File splitModelFile = null;
				LOG.debug("splitModelPath: " + splitModelPath);
				if (splitModelPath!=null) {
					String splitModelRealPath = servletContext.getRealPath(splitModelPath);
					splitModelFile = new File(splitModelRealPath);
				}

				String mergeModelPath = jochreProperties.getProperty("mergeModelPath");
				LOG.debug("mergeModelPath: " + mergeModelPath);
				File mergeModelFile = null;
				if (mergeModelPath!=null) {
					String mergeModelRealPath = servletContext.getRealPath(mergeModelPath);
					mergeModelFile = new File(mergeModelRealPath);
				}
				
				String lexiconServiceClassName = jochreProperties.getProperty("lexiconService");
				LOG.debug("lexiconServiceClassName: " + lexiconServiceClassName);
				@SuppressWarnings("rawtypes")
				Class lexiconServiceClass = Class.forName(lexiconServiceClassName);
				@SuppressWarnings({ "rawtypes", "unchecked" })
				Constructor constructor =
					lexiconServiceClass.getConstructor(new Class[]{});
				LocaleSpecificLexiconService localeSpecificLexiconService = (LocaleSpecificLexiconService) constructor.newInstance();

				String lexiconDirPath = jochreProperties.getProperty("lexiconDirPath");
				LOG.debug("lexiconDirPath: " + lexiconDirPath);
				String lexiconDirRealPath = servletContext.getRealPath(lexiconDirPath);

				localeSpecificLexiconService.setLexiconDirPath(lexiconDirRealPath);
				
				Locale locale = localeSpecificLexiconService.getLocale();
				Lexicon lexicon = localeSpecificLexiconService.getLexicon();
				WordSplitter wordSplitter = localeSpecificLexiconService.getWordSplitter();
				
				MostLikelyWordChooser wordChooser = lexiconService.getMostLikelyWordChooser(lexicon, wordSplitter);

				this.documentHtmlGenerator = new DocumentHtmlGenerator(textGetter);
				
				if (this.currentDoc!=null) {
					this.currentDoc.setFileName(currentFile.getName());
					this.currentDoc.save();
					this.documentGenerator = this.documentService.getJochreDocumentGenerator(this.currentDoc);
					this.documentGenerator.requestSave(currentUser);
				} else {
					this.documentGenerator = this.documentService.getJochreDocumentGenerator(currentFile.getName(), "", locale);
				}
				documentGenerator.requestAnalysis(splitModelFile, mergeModelFile, letterModelFile, wordChooser);
				documentGenerator.addProcessedImageObserver(this.documentHtmlGenerator);
				
				String lowerCaseFileName = currentFile.getName().toLowerCase();
				Thread thread = null;
				if (lowerCaseFileName.endsWith(".pdf")) {
					PdfService pdfService = locator.getPdfServiceLocator().getPdfService();
					PdfImageVisitor pdfImageVisitor = pdfService.getPdfImageVisitor(currentFile, startPage, endPage, documentGenerator);
					this.progressMonitor = pdfImageVisitor.monitorTask();
					this.currentHtmlIndex = 0;
					thread = new Thread(pdfImageVisitor);
					thread.setName(currentFile.getName() + " Processor");
					progressTimer.setRunning(true);
				} else if (lowerCaseFileName.endsWith(".png")
						|| lowerCaseFileName.endsWith(".jpg")
						|| lowerCaseFileName.endsWith(".jpeg")
						|| lowerCaseFileName.endsWith(".gif")) {
					ImageDocumentExtractor extractor = documentService.getImageDocumentExtractor(currentFile, documentGenerator);
					this.progressMonitor = extractor.monitorTask();
					this.currentHtmlIndex = 0;
					thread = new Thread(extractor);
					thread.setName(currentFile.getName() + " Processor");
				} else {
					throw new RuntimeException("Unrecognised file extension");
				}
				thread.start();
				progressTimer.setRunning(true);
				btnAnalyse.setDisabled(true);
			}
		} catch (Exception e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		}
	}
	
	public void onTimer$progressTimer(Event event) {
		if (this.progressMonitor!=null) {
			if (this.documentGenerator!=null) {
				List<Html> htmlList = this.documentHtmlGenerator.getHtmlList();
				if (currentHtmlIndex<htmlList.size()) {
					htmlContent.appendChild(htmlList.get(currentHtmlIndex));
					currentHtmlIndex++;
				}
			}
			if (this.progressMonitor.isFinished()) {
				if (this.progressMonitor.getException()!=null) {
					lblCurrentAction.setValue(Labels.getLabel("imageMonitor.error"));
					errorBox.setVisible(true);
					lblErrorMessage.setValue(LogUtils.getErrorString(this.progressMonitor.getException()));
				} else {
					lblCurrentAction.setValue(Labels.getLabel("imageMonitor.complete"));
				}
				progressMeter1.setValue(100);
				progressTimer.setRunning(false);
			} else {
				double progress = progressMonitor.getPercentComplete()*100;
				if (progress>100) progress = 100;
				progressMeter1.setValue((int) Math.round(progress));
				List<MessageResource> messages = progressMonitor.getCurrentActions();
				String currentAction = "";
				boolean firstAction = true;
				for (MessageResource message : messages) {
					if (!firstAction)
						currentAction += " - ";
					currentAction += Labels.getLabel(message.getKey(), message.getArguments());
					firstAction = false;
				}
				currentAction += "...";
				lblCurrentAction.setValue(currentAction);
				LOG.debug("currentAction: " + currentAction);
				LOG.debug("progress: " + progress);
			}
		}
		
	}
	
	public void onClick$btnDone(Event event) {
		try {
			LOG.debug("onClick$btnDone");
			Executions.sendRedirect("docs.zul");
		} catch (Exception e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		}
	}
	
	private class DocumentHtmlGenerator implements ProcessedImageObserver {
		private TextGetter textGetter;
		private List<Html> htmlList = new ArrayList<Html>();

		public DocumentHtmlGenerator(TextGetter textGetter) {
			super();
			this.textGetter = textGetter;
		}

		public List<Html> getHtmlList() {
			return htmlList;
		}

		@Override
		public void onImageProcessed(JochreImage jochreImage) {
			Html html = new Html();
			StringWriter out = new StringWriter();
			textGetter.getText(jochreImage, out, TextFormat.XHTML);
			out.append("<HR/>");
			html.setContent(out.toString());
			htmlList.add(html);
		}

	}
}
