package frontend;

import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.ui.console.IHyperlink;
import org.eclipse.ui.console.IPatternMatchListenerDelegate;
import org.eclipse.ui.console.PatternMatchEvent;
import org.eclipse.ui.console.TextConsole;

public class FrontendMavenPluginOutputFileMatcher implements IPatternMatchListenerDelegate {

	private static final String FRONTEND_PROBLEM = "frontend.problemmarker";

	static AtomicBoolean cleanupFrontendMarkers = new AtomicBoolean(true);

	private TextConsole textConsole;
	
	@Override
	public void connect(TextConsole textConsole) {
		this.textConsole = textConsole;
		
	}

	void cleanupFrontendMarkers() {
		if (cleanupFrontendMarkers.compareAndSet(true, false)) {
			// Cleanup markers from previous session
			IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
			try {
				IMarker[] markers = workspaceRoot.findMarkers(IMarker.PROBLEM, false, IResource.DEPTH_INFINITE);
				for (IMarker marker : markers) {
					if (marker.getAttribute(FRONTEND_PROBLEM, false)) {
						marker.delete();
					}
				}
			} catch (CoreException e) {
			}
		}
	}

	@Override
	public void disconnect() {
		this.textConsole = null;
		cleanupFrontendMarkers.set(true);
	}

	private long lastTimeMillis = System.currentTimeMillis();
	@Override
	public void matchFound(PatternMatchEvent patternMatchEvent) {
		if (ConsoleInstanceOfPropertyTester.isExpectedConsole(textConsole, "org.eclipse.m2e.core.ui.internal.console.MavenConsole")) {
			long currentTimeMillis = System.currentTimeMillis();
			if (currentTimeMillis - lastTimeMillis > 60000) {
				cleanupFrontendMarkers.set(true);
			}
			lastTimeMillis = currentTimeMillis;
		}
		cleanupFrontendMarkers();
		TextConsole textConsole = (TextConsole) patternMatchEvent.getSource();
		try {
			int offset = patternMatchEvent.getOffset();
			int length = patternMatchEvent.getLength();

			String linkText = textConsole.getDocument().get(offset, length);
			String fileName = FrontendMavenBuildOutputFileHyperlink.getFileName(linkText);
			if (fileName != null) {

				try {
						IFile resource = (IFile) FrontendMavenBuildOutputFileHyperlink.getResource(fileName);
						if (resource != null) {
						FrontendMavenBuildOutputFileHyperlink.MESSAGE_TYPE messageType = FrontendMavenBuildOutputFileHyperlink
								.getMessageType(linkText);
						int lineNumber = FrontendMavenBuildOutputFileHyperlink.getLineNumber(linkText);
						IMarker m = resource.createMarker(IMarker.PROBLEM);
						m.setAttribute(FRONTEND_PROBLEM, true);
						m.setAttribute(IMarker.LINE_NUMBER, lineNumber);
						String message = FrontendMavenBuildOutputFileHyperlink.getMessage(linkText);
						if (message == null) {
							m.setAttribute(IMarker.MESSAGE, "frontend build error");
						} else {
							m.setAttribute(IMarker.MESSAGE, message);
						}
						if (messageType == FrontendMavenBuildOutputFileHyperlink.MESSAGE_TYPE.ERROR) {
							m.setAttribute(IMarker.PRIORITY, IMarker.PRIORITY_HIGH);
							m.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
						} else {
							m.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_WARNING);
						}
					}
				} catch (CoreException e) {
				}
			}

			IHyperlink link = new FrontendMavenBuildOutputFileHyperlink(textConsole);
			textConsole.addHyperlink(link, offset, length);
		} catch (BadLocationException e) {
		}
	}

}
