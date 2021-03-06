package frontend;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.IHyperlink;
import org.eclipse.ui.console.TextConsole;
import org.eclipse.ui.ide.IDE;

/**
 * A hyperlink from a stack trace line of the form "at file.ext(l,c)"
 */
public class FrontendMavenBuildOutputFileHyperlink implements IHyperlink {
	static Pattern ESLintPattern = Pattern.compile("(WARNING|ERROR)..39m at (.*)\\((\\d+),(\\d+)\\):[\\r\\n][\\r\\n]?.*\\[INFO\\].+33m(.+).\\[39m$", Pattern.MULTILINE);
	static Pattern JSHintPattern = Pattern.compile("\\[INFO\\] ([^ ]+): line (\\d+), col (\\d+), ?(.*)$");

	enum MESSAGE_TYPE {
		UNKNOWN, INFO, WARNING, ERROR
	};

	private TextConsole fConsole;

	/**
	 * Constructor for PythonFileHyperlink.
	 */
	public FrontendMavenBuildOutputFileHyperlink(TextConsole console) {
		fConsole = console;
	}

	/**
	 * @see org.eclipse.debug.ui.console.IConsoleHyperlink#linkEntered()
	 */
	public void linkEntered() {
	}

	/**
	 * @see org.eclipse.debug.ui.console.IConsoleHyperlink#linkExited()
	 */
	public void linkExited() {
	}

	/**
	 * @see org.eclipse.debug.ui.console.IConsoleHyperlink#linkActivated()
	 */
	public void linkActivated() {
		String fileName;
		int lineNumber;
		String linkText = getLinkText();
		fileName = getFileName(linkText);
		lineNumber = getLineNumber(linkText);

		// documents start at 0
		if (fileName != null) {
			try {
				IFile sourceFile = (IFile) getResource(fileName);
				IWorkbenchPage activePage = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
				if (activePage != null) {
					IMarker marker = sourceFile.createMarker(IMarker.TEXT);
					HashMap<String, Object> map = new HashMap<String, Object>();
					if (lineNumber != -1) {
						map.put(IMarker.LINE_NUMBER, new Integer(lineNumber));
					}
					marker.setAttributes(map);
					IDE.openEditor(activePage, marker);
					marker.delete();
				}
			} catch (CoreException e) {
			}
		}

	}

	/**
	 * @see IDebugModelPresentation#getEditorId(IEditorInput, Object)
	 */
	public String getEditorId(IEditorInput input, Object inputObject) {
		try {
			IEditorDescriptor descriptor = IDE.getEditorDescriptor(input.getName());
			return descriptor.getId();
		} catch (PartInitException e) {
			return null;
		}
	}

	/**
	 * @see IDebugModelPresentation#getEditorInput(Object)
	 */
	public IEditorInput getEditorInput(Object item) {
		return null;
	}

	static IResource getResource(String fileName) throws CoreException {
		IFile f = ResourcesPlugin.getWorkspace().getRoot().getFileForLocation(new Path(fileName));
		return f;
	}

	static MESSAGE_TYPE getMessageType(String linkText) {
		Matcher m = ESLintPattern.matcher(linkText);
		if (m.find()) {
			String type = m.group(1);
			MESSAGE_TYPE message_type = MESSAGE_TYPE.valueOf(type);
			return (message_type == null ? MESSAGE_TYPE.WARNING : message_type);
		}

		return MESSAGE_TYPE.WARNING;
	}
	
	/**
	 * Returns the fully qualified name of the type to open
	 *
	 * @return fully qualified type name
	 * @exception CoreException
	 *                if unable to parse the type name
	 */
	static String getFileName(String linkText) {
		Matcher m = ESLintPattern.matcher(linkText);
		if (m.find()) {
			String name = m.group(2);
			return name;
		}

		m = JSHintPattern.matcher(linkText);
		if (m.find()) {
			String name = m.group(1);
			return name;
		}

		return "";
	}

	/**
	 * Returns the line number associated with the stack trace or -1 if none.
	 *
	 */
	static int getLineNumber(String linkText) {
		Matcher m = ESLintPattern.matcher(linkText);
		if (m.find()) {
			String lineNumberText = m.group(3);
			try {
				return Integer.parseInt(lineNumberText);
			} catch (NumberFormatException e) {
			}
		}

		m = JSHintPattern.matcher(linkText);
		if (m.find()) {
			String lineNumberText = m.group(2);
			try {
				return Integer.parseInt(lineNumberText);
			} catch (NumberFormatException e) {
			}
		}

		return -1;
	}

	/**
	 * Returns the column number associated with the stack trace or -1 if none.
	 *
	 */
	static int getColumnNumber(String linkText) {
		Matcher m = ESLintPattern.matcher(linkText);
		if (m.find()) {
			String columnNumberText = m.group(4);
			try {
				return Integer.parseInt(columnNumberText);
			} catch (NumberFormatException e) {
			}
		}
		m = JSHintPattern.matcher(linkText);
		if (m.find()) {
			String columnNumberText = m.group(3);
			try {
				return Integer.parseInt(columnNumberText);
			} catch (NumberFormatException e) {
			}
		}
		return -1;
	}
	
	static String getMessage(String linkText) {
		Matcher m = ESLintPattern.matcher(linkText);
		if (m.find()) {
			return m.group(5);
		}
		m = JSHintPattern.matcher(linkText);
		if (m.find()) {
			return m.group(4);			
		}
		return null;
	}

	/**
	 * Returns the console this link is contained in.
	 *
	 * @return console
	 */
	protected TextConsole getConsole() {
		return fConsole;
	}

	/**
	 * Returns this link's text
	 *
	 * @exception CoreException
	 *                if unable to retrieve the text
	 */
	protected String getLinkText() {
		try {
			IDocument document = getConsole().getDocument();
			IRegion region = getConsole().getRegion(this);
			int regionOffset = region.getOffset();
			int regionLength = region.getLength();

			String line = document.get(regionOffset, regionLength);

			return line.trim();
		} catch (BadLocationException e) {
		}
		return "";
	}

}
