package frontend;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.ui.console.IConsole;

/**
 * Tests if an IOConsole's class name or superclass name or interfaces name matches the expected value
 *
 * @since 3.1
 */
public class ConsoleInstanceOfPropertyTester extends PropertyTester {
	
	public ConsoleInstanceOfPropertyTester() {
		
	}

    /* (non-Javadoc)
     * @see org.eclipse.core.expressions.IPropertyTester#test(java.lang.Object, java.lang.String, java.lang.Object[], java.lang.Object)
     */
	@Override
	public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
        IConsole console = (IConsole) receiver;
        return isExpectedConsole(console, expectedValue);
    }

	static boolean isExpectedConsole(IConsole console, Object expectedValue) {
		Class<? extends IConsole> clazz = console.getClass();
        String className = clazz.getName();
        if (className.equals(expectedValue)) {
        	return true;
        }
        className = clazz.getSuperclass().getName();
        if (className.equals(expectedValue)) {
        	return true;
        }
        Class<?>[] interfaces = clazz.getInterfaces();
        for (Class<?> interfaze : interfaces) {
        	className = interfaze.getName();
            if (className.equals(expectedValue)) {
            	return true;
            }
		}
        return false;
	}

}
