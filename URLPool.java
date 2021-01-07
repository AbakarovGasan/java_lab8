import java.util.*;

/** 
 * Класс пула URL-адресов для хранения списка URL-адресов для поиска по глубине.
 * Сохраняется как экземпляр URLDepthPair.
 */
public class URLPool {
    
    /** Связанный список для представления ожидающих URL-адресов. */
    private LinkedList<URLDepthPair> pendingURLs;
    
    /** Связанный список для представления обработанных URL-адресов. */
    public LinkedList<URLDepthPair> processedURLs;
    
    /** Число обрабатываемых в текущий момент URL-адресов */
    private int processingURLsCount = 0;
    
    /** Максимальная глубина */
    private int maxDepth = 0;
    
    public URLPool() {
        pendingURLs = new LinkedList<URLDepthPair>();
        processedURLs = new LinkedList<URLDepthPair>();
    }
    
    /** возврашает false, если все адреса обработаны */
    public synchronized boolean needWait(){
        return (pendingURLs.size()+processingURLsCount)!=0;
    }
    
    public synchronized void setMaxDepth(int depth){
        this.maxDepth += depth;
    }
    
    /** Синхронизированный метод добавления пары в пул. */
    public synchronized boolean put(URLDepthPair depthPair) {
        boolean added = false;
        // Если глубина меньше максимальной, добавляем пару в пул.
        if (depthPair.getDepth() < this.maxDepth) {
            pendingURLs.addLast(depthPair);
            added = true;
            this.notify();
        }
        // Если глубина не меньше максимальной, просто добавляем пару 
         // к просмотренному списку.
        return added;
        }

    /**
     * метод, который прослеживает, что адрес был обработан
     */
    public synchronized void finished(){
        processingURLsCount -= 1;
    }
    public synchronized URLDepthPair get() {
        
        URLDepthPair myDepthPair = null;
        
        //Пока пул пуст, подождать.
        if (pendingURLs.size() == 0) {
            try {
                this.wait();
            }
            catch (InterruptedException e) {
                System.err.println("MalformedURLException: " + e.getMessage());
                return null;
            }
        } 
        // Удаляем первую пару глубин, добавляем к просмотренным и обработанным URL,
         // и вернем его.
        myDepthPair = pendingURLs.removeFirst();
        processingURLsCount += 1;
        processedURLs.add(myDepthPair);
        return myDepthPair;
    }
}
