package frontend;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
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
	private static Pattern ESLintPattern = Pattern.compile("at (.*)\\((\\d+),(\\d+)\\):$");
	private static Pattern JSHintPattern = Pattern.compile(" ([^ ]+): line (\\d+), col (\\d+),");

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
				IFile sourceFile = getSourceModule(fileName);
				IWorkbenchPage activePage = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
				if (activePage != null) {
					IMarker marker = sourceFile.createMarker(IMarker.TEXT);
					HashMap<String, Object> map = new HashMap<String, Object>();
					if (lineNumber != -1) {
						map.put(IMarker.LINE_NUMBER, new Integer(lineNumber));
					}
					map.put(IDE.EDITOR_ID_ATTR, "org.eclipse.ui.DefaultTextEditor");
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

	protected IFile getSourceModule(String fileName) throws CoreException {
		IFile f = ResourcesPlugin.getWorkspace().getRoot().getFileForLocation(new Path(fileName));
		return f;
	}

	/**
	 * Returns the fully qualified name of the type to open
	 *
	 * @return fully qualified type name
	 * @exception CoreException
	 *                if unable to parse the type name
	 */
	protected String getFileName(String linkText) {
		Matcher m = ESLintPattern.matcher(linkText);
		if (m.find()) {
			String name = m.group(1);
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
	protected int getLineNumber(String linkText) {
		Matcher m = ESLintPattern.matcher(linkText);
		if (m.find()) {
			String lineNumberText = m.group(2);
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
	protected int getColumnNumber(String linkText) {
		Matcher m = ESLintPattern.matcher(linkText);
		if (m.find()) {
			String columnNumberText = m.group(3);
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

			int lineNumber = document.getLineOfOffset(regionOffset);
			IRegion lineInformation = document.getLineInformation(lineNumber);
			int lineOffset = lineInformation.getOffset();
			String line = document.get(lineOffset, lineInformation.getLength());

			return line.trim();
		} catch (BadLocationException e) {
		}
		return "";
	}

}
