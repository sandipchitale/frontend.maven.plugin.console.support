package frontend;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.ui.console.IHyperlink;
import org.eclipse.ui.console.IPatternMatchListenerDelegate;
import org.eclipse.ui.console.PatternMatchEvent;
import org.eclipse.ui.console.TextConsole;

public class FrontendMavenPluginOutputFileMatcher implements IPatternMatchListenerDelegate {
	@Override
	public void connect(TextConsole textConsole) {
	}

	@Override
	public void disconnect() {
	}

	@Override
	public void matchFound(PatternMatchEvent patternMatchEvent) {
		TextConsole textConsole = (TextConsole) patternMatchEvent.getSource();
		try {
            int offset = patternMatchEvent.getOffset();
            int length = patternMatchEvent.getLength();
            IHyperlink link = new FrontendMavenBuildOutputFileHyperlink(textConsole);
            textConsole.addHyperlink(link, offset, length);
        } catch (BadLocationException e) {
        }
	}

}
