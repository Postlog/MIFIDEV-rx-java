package rx;

public class BasicDisposable extends AbstractDisposable {
    public static Disposable empty() {
        return new BasicDisposable();
    }
}
