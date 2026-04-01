package rx;

public interface ObservableOnSubscribe<T> {
    void subscribe(ObservableEmitter<T> emitter) throws Exception;
}
