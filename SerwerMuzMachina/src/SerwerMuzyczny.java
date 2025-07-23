import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;


public class SerwerMuzyczny {
    private final List<ObjectOutputStream> strumienieWyjDoKlientow = new ArrayList<>();

    public static void main(String[] args) {
        new SerwerMuzyczny().doRoboty();
    }

    public void doRoboty() {
        try {
            ServerSocket gniazdoSerwera = new ServerSocket(4242);
            ExecutorService pulaWatkow = Executors.newCachedThreadPool();

            while (!gniazdoSerwera.isClosed()) {
                Socket gniazdoKlienta = gniazdoSerwera.accept();
                ObjectOutputStream wyj = new ObjectOutputStream(gniazdoKlienta.getOutputStream());
                strumienieWyjDoKlientow.add(wyj);

                ObslugaKlienta doObslugiKlienta = new ObslugaKlienta(gniazdoKlienta);
                pulaWatkow.execute(doObslugiKlienta);
                System.out.println("Mamy połączenie");
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public void przekazDoWszystkich(Object nazwaIKomunikat, Object sekwencjaTaktow){
        for (ObjectOutputStream wyjsciowyStrumienKlienta : strumienieWyjDoKlientow) {
            try {
                wyjsciowyStrumienKlienta.writeObject(nazwaIKomunikat);
                wyjsciowyStrumienKlienta.writeObject(sekwencjaTaktow);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    public class ObslugaKlienta implements Runnable {
        private ObjectInputStream wej;

        public ObslugaKlienta(Socket gniazdo) {
            try {
                wej = new ObjectInputStream(gniazdo.getInputStream());
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

        public void run() {
            Object nazwaUzytkownikaIKomunikat;
            Object sekwencjaTaktow;

            try{
                while((nazwaUzytkownikaIKomunikat = wej.readObject()) != null){
                    sekwencjaTaktow = wej.readObject();

                    System.out.println("Odczyt dwóch obiektów");
                    przekazDoWszystkich(nazwaUzytkownikaIKomunikat, sekwencjaTaktow);
                }
            }catch (IOException | ClassNotFoundException ex) {
                ex.printStackTrace();
            }
        }
    }

}