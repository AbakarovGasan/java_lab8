import java.util.*;

/** CrawlerTask реализует интерфейс Runnable. Каждый экземпляр имеет
  * ссылкy на экземпляр класса URLPool. Получает пару глубин URL из
  * пул (ожидает, если недоступен), получает веб-страницу, получает все
  * URL-адреса со страницы и добавляет новую пару URLDepth в пул URL-адресов для
  * каждой найденной URL.
  **/
public class CrawlerTask implements Runnable {    
    /** Поле для пула URL */
    public URLPool myPool;
    
    /** Конструктор для установки пула URL-адресов переменной в пул, переданный методу */    
    public CrawlerTask(URLPool pool) {
        myPool = pool;
    }
    
    /** Метод для запуска задач CrawlerTask */
    public void run() {

        // Взять пару глубины из пула.
        URLDepthPair depthPair = myPool.get();
        
        // Глубина из пары.
        int myDepth = depthPair.getDepth();
        
        // Получить все ссылки с сайта и сохранить их в новом связанном списке.
        LinkedList<String> linksList = new LinkedList<String>();
        linksList = Crawler.getAllLinks(depthPair);
        
        // Перебирать все сохраненные ссылки.
        for (String newURL : linksList) {
            
            // Создать новую пару для каждой найденной ссылки и добавить ее в пул.
            URLDepthPair newDepthPair = new URLDepthPair(newURL, myDepth + 1);
            myPool.put(newDepthPair);
        }
        // послать сообщение о завершении
        myPool.finished();
    }
}
