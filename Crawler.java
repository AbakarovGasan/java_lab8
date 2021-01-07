import java.net.*;
import java.util.*;
import java.io.*;

/**
 * Этот класс обрабатывает аргументы командной строки, 
 * создает экземпляр пула URL-адресов, добавляет введенный 
 * URL-адрес в пул и создает количество задач искателя 
 * введенных с потоками для их выполнения. 
 * Затем, когда сканирование завершено, 
 * распечатывает список найденных URL-адресов.
 */
public class Crawler {
    
    
    public static void main(String[] args) {
        // Переменные для текущей глубины и запрошенного количества потоков.
        int depth = 0;
        int numThreads = 0;
        
        // Проверить правильность длины ввода.
        if (args.length != 3) {
            System.out.println("usage: java Crawler <URL> <depth> <number of crawler threads>");
            System.exit(1);
        }
        else {
            try {
        // Преобразовать строковый аргумент в целочисленное значение.
                depth = Integer.parseInt(args[1]);
                numThreads = Integer.parseInt(args[2]);
            }
            catch (NumberFormatException nfe) {
            // если вышла ошибка, то вывести "глубина и количество потоков должны быть
            // целочисленными значениями
                System.out.println("depth and number of crawler threads must be an integers");
                System.exit(1);
            }
        }
        
        // Пары глубины и URL-адреса для представления веб-сайта, введенного пользователем.
        // с глубиной 0 
        URLDepthPair currentDepthPair = new URLDepthPair(args[0], 0);
        
        // Создать пул URL-адресов 
        URLPool pool = new URLPool();
        // присвоить максимальную глубину
        pool.setMaxDepth(depth);
        // добавить введенный пользователем веб-сайт.
        pool.put(currentDepthPair);
        

        // Переменные для общего количества потоков и начальных потоков.
        int totalThreads = 0;
        int initialActive = Thread.activeCount();
        
         // Выполнить цикл до тех пор, пока нужно ждать 
         // если общее количество потоков меньше запрошенного количества потоков,
         // создаем больше потоков и запускаем их в CrawlerTask.
        while (pool.needWait()) {
            if (Thread.activeCount() - initialActive < numThreads) {
                CrawlerTask crawler = new CrawlerTask(pool);
                new Thread(crawler).start();
            }
            else {
                try {
                    Thread.sleep(100);  // ожидать 0.1 секунд
                }
                //
                catch (InterruptedException ie) {
                    System.out.println("Caught unexpected " +
                                       "InterruptedException, ignoring...");
                }

            }
        }
                
        // Распечатайте все обработанные URL-адреса с глубиной
        Iterator<URLDepthPair> iter = pool.processedURLs.iterator();
        while (iter.hasNext()) {
            System.out.println(iter.next());
        }        // Exit.
        System.exit(0);
        


    }

     // Метод, который принимает URLDepthPair и возвращает cписок ссылок на сайте
    public static LinkedList<String> getAllLinks(URLDepthPair myDepthPair) {
        
        // Инициализировать список
        LinkedList<String> URLs = new LinkedList<String>();
        
        // Инициализировать сокет.
        Socket sock;
        
        // Попытаться создать новый сокет с URL-адресом, переданным методу в
         // URLDepthPair и портом 80.
        try {
            sock = new Socket(myDepthPair.getWebHost(), 80);
        }
        // Вывести все возможные ошибки и вернуть cписок.
        catch (UnknownHostException e) {
            System.err.println("UnknownHostException: " + e.getMessage());
            return URLs;
        }
        catch (IOException e) {
            System.err.println("IOException: " + e.getMessage());
            return URLs;
        }
        
        // Попробовать установить сокет на тайм-аут через 3 секунды.
        try {
            sock.setSoTimeout(3000);
        }
        // Вывести все возможные ошибки и вернуть cписок.
        catch (SocketException e) {
            System.err.println("SocketException: " + e.getMessage());
            return URLs;
        }
        
        // поток вывода.
        OutputStream outStream;
        
        // Попытка получить поток вывода от сокета. 
        try {
            outStream = sock.getOutputStream();
        }
        // Вывести все возможные ошибки и вернуть cписок.
        catch (IOException e) {
            System.err.println("IOException: " + e.getMessage());
            return URLs;
        }
        
        // Инициализирует PrintWriter. True означает, что PrintWriter будет
        // сброшен после каждого вывода.
        PrintWriter myWriter = new PrintWriter(outStream, true);
        
        // отправить запрос.  
        myWriter.println("GET " + myDepthPair.getDocPath() + " HTTP/1.1");
        myWriter.println("Host: " + myDepthPair.getWebHost());
        myWriter.println("Connection: close");
        myWriter.println();

        // поток ввода.
        InputStream inStream;
        
        // Попытка получить поток вывода от сокета.  
        try {
            inStream = sock.getInputStream();
        }
        // Вывести все возможные ошибки и вернуть cписок.
        catch (IOException excep){
            System.err.println("IOException: " + excep.getMessage());
            return URLs;
        }
        // создать буфферы для потоков ввода   
        InputStreamReader inStreamReader = new InputStreamReader(inStream);
        BufferedReader BuffReader = new BufferedReader(inStreamReader);
        
        // Попытаться прочитать строку из буффера ввода. 
        while (true) {
            String line;
            try {
                line = BuffReader.readLine();
            }
            // Вывести все возможные ошибки и вернуть cписок.
            catch (IOException except) {
                System.err.println("IOException: " + except.getMessage());
                return URLs;
            }
            // Прочитали документ
            if (line == null)
                break;
        
            
            // Переменные для представления индексов, в которых ссылки начинаются
            // и заканчиваются как и текущий индекс.
            int beginIndex = 0;
            int endIndex = 0;
            int index = 0;
            
            while (true) {
                // Ищем URL_INDICATOR в текущей строке.
                index = line.indexOf(URL_INDICATOR, index);
                if (index == -1) // -1 означает, что URL_INDICATOR не найден
                    break;
                
                // Переместить текущий индекс вперед и установить значение beginIndex.
                index += URL_INDICATOR.length();
                beginIndex = index;
                
                // Найти конец в текущей строке и установить значение endIndex.
                endIndex = line.indexOf(END_URL, index);
                index = endIndex;
                
                // Устанавливаем ссылку на подстроку между начальным индексом
                 // и конечным индексом. Добавить в наш список URL-адресов.
                String newLink = line.substring(beginIndex, endIndex);
                
                if (newLink.indexOf("://")==-1){
                    //если ссылка не имеет "://", 
                    //то это есть продолжение старой ссылки
                    newLink = myDepthPair.getURL() + newLink; // 
                }
                
                URLs.add(newLink);
            }
            
        }
        // Вернуть список
        return URLs;
    }
    
    /**
                 * Константа для строки, указывающей ссылку.
                 */
        public static final String URL_INDICATOR = "a href=\"";
                
                /**
                 * Константа для строки, указывающей конец веб-хостинга и
                 * начало docpath.
                 */
        public static final String END_URL = "\"";
    
}

