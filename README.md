# Реализация библиотеки RxJava (Клон)

## Запуск и подготовка проекта

Проект использует фреймворк **Maven** для сборки и управления зависимостями, а также включает удобный **Makefile** для быстрого выполнения команд. 
Для работы потребуется установленный **Java JDK 17** и **Maven**.

### Доступные команды:
* `make compile` или `mvn compile` — скомпилировать исходные коды проекта.
* `make test` или `mvn test` — запустить все юнит-тесты (рекомендуется для проверки правильности реализации).
* `make package` или `mvn package` — собрать проект в `.jar` файл.
* `make clean` или `mvn clean` — очистить проект от скомпилированных файлов.

Чтобы полностью убедиться в работоспособности базовых компонентов, операторов и Schedulers, достаточно выполнить:
```bash
make test
```
Все тесты (находящиеся в `src/test/java/rx/`) должны отработать успешно, продемонстрировав многопоточную работу, обработку исключений и корректность операторов.

---


## 1. Архитектура системы

Реализованная система представляет собой упрощенную версию реактивной библиотеки (подобной RxJava 2/3), использующей паттерн «Наблюдатель» (Observer/Observable). 

### Основные компоненты:
* **Observable<T>** — класс(издатель), предоставляющий поток данных. Управляет подписками и описывает цепочку операторов через создание новых экземпляров `Observable`, которые оборачивают исходный.
* **Observer<T>** — интерфейс наблюдателя. Содержит 3 метода: `onNext(T)` для получения элементов, `onError(Throwable)` для обработки ошибок, и `onComplete()` для успешного завершения потока.
* **Disposable** — интерфейс для управления подпиской, позволяющий проверить статус подписки (`isDisposed()`) и отменить ее (`dispose()`).
* **ObservableEmitter<T>** — расширяет `Observer` и `Disposable`, предоставляя источник данных для метода `Observable.create`.
* **Scheduler** — интерфейс, абстрагирующий процесс выполнения задач в различных потоках с помощью метода `execute(Runnable)`.

### Операторы:
Для реализации функциональности преобразования и манипулирования потоками, методы-операторы `map`, `filter`, `flatMap`, `subscribeOn` и `observeOn` возвращают новые экземпляры `Observable`. Внутри используется паттерн «Декоратор»: новый `Observer` подписывается на исходный `Observable` и перехватывает его события (`onNext`, `onError`, `onComplete`), модифицируя их или перенаправляя в другой поток перед передачей конечному подписчику.

---

## 2. Принципы работы Schedulers

`Scheduler` обеспечивает абстракцию над пулами потоков, позволяя менять контекст выполнения реактивных цепочек:
* **IOThreadScheduler** (аналог `Schedulers.io()`): использует `CachedThreadPool`. Подходит для I/O операций (работа с сетью, чтением/записью файлов), так как пул динамически создает потоки по мере необходимости и переиспользует простаивающие.
* **ComputationScheduler** (аналог `Schedulers.computation()`): использует `FixedThreadPool` с числом потоков, равным количеству доступных ядер процессора (`Runtime.getRuntime().availableProcessors()`). Оптимизирован для вычислительно сложных задач, чтобы избежать накладных расходов на переключение контекста при избыточном количестве потоков.
* **SingleThreadScheduler** (аналог `Schedulers.single()`): использует `SingleThreadExecutor`. Гарантирует последовательное выполнение задач строго в одном фоновом потоке, что полезно для операций, не поддерживающих конкурентный доступ.

### Управление потоками:
* **subscribeOn(Scheduler)** — меняет поток, в котором происходит *подписка* на источник. Вызов `source.subscribe(...)` заворачивается в задачу для `Scheduler`.
* **observeOn(Scheduler)** — меняет поток, в котором *наблюдатель* (Observer) будет получать элементы и события от потока. Все вызовы `onNext()`, `onError()` и `onComplete()` перенаправляются через механизм `Scheduler`.

---

## 3. Процесс тестирования

Тестирование выполнено с использованием фреймворка **JUnit 5**. В ходе работы написаны два тестовых класса:
1. **ObservableTest** — проверяет синхронную работу компонентов:
    * `testCreateAndSubscribe`: проверка базовой генерации элементов и вызова `onComplete`.
    * `testMap`, `testFilter`: проверка корректности преобразования значения и отсеивания элементов по предикату.
    * `testFlatMap`: проверка разворачивания вложенных `Observable`.
    * `testErrorHandling`: проверка перехвата исключений и корректной передачи их в метод `onError`.
    * `testDisposable`: проверка логики прерывания потока при отмене подписки (через проверку `isDisposed()`).
2. **SchedulerTest** — проверяет асинхронную работу операторов:
    * `testSubscribeOn`: с помощью `CountDownLatch` блокируется основной поток до завершения событий. Проверяется, что подписка (и создание элементов) происходят в пуле `IOThreadScheduler` (отличном от `main`).
    * `testObserveOn`: проверяется, что элементы генерируются в основном потоке, а события доходят до `Observer` в потоке из пула `ComputationScheduler`.

---

## 4. Примеры использования реализованной библиотеки

### Базовое создание и операторы:
```java
Observable.create((ObservableEmitter<Integer> emitter) -> {
    emitter.onNext(1);
    emitter.onNext(2);
    emitter.onNext(3);
    emitter.onComplete();
})
.filter(x -> x % 2 != 0) // оставит 1 и 3
.map(x -> "Number: " + x)
.subscribe(new Observer<String>() {
    @Override
    public void onNext(String item) { System.out.println(item); }
    @Override
    public void onError(Throwable t) { t.printStackTrace(); }
    @Override
    public void onComplete() { System.out.println("Done!"); }
});
```

### Асинхронное выполнение с Schedulers:
```java
Observable.create(emitter -> {
    // Выполнится в IO-потоке благодаря subscribeOn
    System.out.println("Generating in: " + Thread.currentThread().getName());
    emitter.onNext("Data from DB");
    emitter.onComplete();
})
.subscribeOn(Schedulers.io())
.observeOn(Schedulers.computation()) // Обработка результата переключается в Computation-поток
.subscribe(new Observer<String>() {
    @Override
    public void onNext(String item) {
        System.out.println("Received in: " + Thread.currentThread().getName() + " -> " + item);
    }
    @Override
    public void onError(Throwable t) {}
    @Override
    public void onComplete() {}
});
```

### Обработка ошибок:
```java
Observable.create(emitter -> {
    emitter.onNext("A");
    emitter.onError(new RuntimeException("Something went wrong"));
    emitter.onNext("B"); // Не будет доставлено
})
.subscribe(new Observer<String>() {
    @Override
    public void onNext(String item) { System.out.println(item); }
    @Override
    public void onError(Throwable t) { System.err.println("Error: " + t.getMessage()); }
    @Override
    public void onComplete() { System.out.println("Done!"); }
});
```
