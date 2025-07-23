import javax.sound.midi.*;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static javax.sound.midi.ShortMessage.*;

// Klasa dziedzicząca po JPanel do rysowania
public class MuzMachina {
    private JList<String> listaOtrzymanych;
    private JTextArea komunikatUzytkownika;
    private ArrayList<JCheckBox> listaPolWyboru;

    private Vector<String> wektorLista = new Vector<>();
    private HashMap<String, boolean[]> mapaOdebranychKompozycji = new HashMap<>();

    private String uzytkownik;
    private int nastepnyNum;

    private ObjectOutputStream wyj;
    private ObjectInputStream wej;

    private Sequencer sekwenser;
    private Sequence sekwencja;
    private Track sciezka;

    String[] nazwyInstrumentow = {"Bass Drum", "Closed Hi-Hat", "Open Hi-Hat", "Acoustic Snare", "Crash Cymbal", "Hand Clap",
    "Cowbell", "Vibraslap", "Low-mid Tom", "High Agogo", "Open Hi Conga"};
    int[] instrumenty = {35, 42, 46, 38, 49, 39, 50, 60, 70, 72, 64, 56, 58, 47, 67, 63};

    public static void main(String[] args) {
        String imie;

        if(args.length > 0){
            imie = args[0];
        }else {
            System.out.print("Podaj nazwę użytkownika: ");
            Scanner sc = new Scanner(System.in);
            imie = sc.nextLine();
        }

        new MuzMachina().inicjalizacjaAplikacji(imie);
    }

    public void inicjalizacjaAplikacji(String imie) {
        uzytkownik = imie;

        try{
            Socket gniazdo = new Socket("127.0.0.1", 4242);
            wyj = new ObjectOutputStream(gniazdo.getOutputStream());
            wej = new ObjectInputStream(gniazdo.getInputStream());
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.submit(new CzytelnikZdalnychDanych());
        } catch (Exception ex){
            System.out.println("Nie można nawiązać połączenia - będziesz grał samemu.");
        }
        kofigurujMidi();
        tworzGUI();
    }

    public void tworzGUI() {
        JFrame ramkaGlowna = new JFrame("MuzMachina");
        ramkaGlowna.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        BorderLayout uklad = new BorderLayout();
        JPanel panelTla = new JPanel(uklad);
        panelTla.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        Box obszarPrzyciskow = new Box(BoxLayout.Y_AXIS);

        JButton start = new JButton("Start");
        start.addActionListener(e -> utworzSciezkeIOdtworz());
        obszarPrzyciskow.add(start);

        JButton stop = new JButton("Stop");
        stop.addActionListener(e -> sekwenser.stop());
        obszarPrzyciskow.add(stop);

        JButton tempoSzb = new JButton("Szybciej");
        tempoSzb.addActionListener(e -> zmienTempo(1.03f));
        obszarPrzyciskow.add(tempoSzb);

        JButton tempoWol = new JButton("Wolniej");
        tempoWol.addActionListener(e -> zmienTempo(0.97f));
        obszarPrzyciskow.add(tempoWol);

        JButton wyslij = new JButton("Wyslij");
        wyslij.addActionListener(e -> wyslijWiadomoscSciezki());
        obszarPrzyciskow.add(wyslij);

        komunikatUzytkownika = new JTextArea();
        komunikatUzytkownika.setLineWrap(true);
        komunikatUzytkownika.setWrapStyleWord(true);
        JScrollPane przewijanieKomunikatu = new JScrollPane(komunikatUzytkownika);
        obszarPrzyciskow.add(przewijanieKomunikatu);

        listaOtrzymanych = new JList<>();
        listaOtrzymanych.addListSelectionListener(new WyborZListyListener());
        listaOtrzymanych.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane lista = new JScrollPane(listaOtrzymanych);
        obszarPrzyciskow.add(lista);
        listaOtrzymanych.setListData(wektorLista);

        Box obszarNazw = new Box(BoxLayout.Y_AXIS);
        for (String nazwaInstrumentu : nazwyInstrumentow) {
            JLabel etykietaInstrumentu = new JLabel(nazwaInstrumentu);
            etykietaInstrumentu.setBorder(BorderFactory.createEmptyBorder(4, 1, 4, 1));
            obszarNazw.add(etykietaInstrumentu);
        }

        panelTla.add(BorderLayout.EAST, obszarPrzyciskow);
        panelTla.add(BorderLayout.WEST, obszarNazw);

        ramkaGlowna.add(panelTla);
        GridLayout siatkaPolWyboru = new GridLayout(16, 16);
        siatkaPolWyboru.setVgap(1);
        siatkaPolWyboru.setHgap(2);

        JPanel panelGlowny = new JPanel(siatkaPolWyboru);
        panelTla.add(BorderLayout.CENTER, panelGlowny);

        listaPolWyboru = new ArrayList<>();
        for (int i = 0; i < 256; i++) {
            JCheckBox c = new JCheckBox();
            c.setSelected(false);
            listaPolWyboru.add(c);
            panelGlowny.add(c);
        }

        ramkaGlowna.setBounds(50,50,300,300);
        ramkaGlowna.pack();
        ramkaGlowna.setVisible(true);
    }

    private void kofigurujMidi() {
        try {
            sekwenser = MidiSystem.getSequencer();
            sekwenser.open();
            sekwencja = new Sequence(Sequence.PPQ, 4);
            sciezka = sekwencja.createTrack();
            sekwenser.setTempoInBPM(120);

        } catch (Exception ex){
            ex.printStackTrace();
        }
    }

    private void utworzSciezkeIOdtworz() {
        int[] listaSciezek;

        sekwencja.deleteTrack(sciezka);
        sciezka = sekwencja.createTrack();

        for (int i = 0; i < 16; i++) {
            listaSciezek = new int[16];

            int klucz = instrumenty[i];

            for (int j = 0; j < 16; j++) {
                JCheckBox jc = listaPolWyboru.get(j + 16 * i);
                if (jc.isSelected()) {
                    listaSciezek[j] = klucz;
                } else {
                    listaSciezek[j] = 0;
                }
            }
            utworzSciezki(listaSciezek);
            sciezka.add(tworzZdarzenie(CONTROL_CHANGE, 1, 127, 0, 16));
        }

        sciezka.add(tworzZdarzenie(PROGRAM_CHANGE, 9, 1, 0, 15));

        try{
            sekwenser.setSequence(sekwencja);
            sekwenser.setLoopCount(sekwenser.LOOP_CONTINUOUSLY);
            sekwenser.setTempoInBPM(120);
            sekwenser.start();
        } catch (Exception ex){
            ex.printStackTrace();
        }
    }

    private void zmienTempo(float mnoznikTempa) {
        float wspolczynnikTempa = sekwenser.getTempoFactor();
        sekwenser.setTempoFactor(wspolczynnikTempa * mnoznikTempa);
    }

    private void wyslijWiadomoscSciezki() {
        boolean[] stanPolaWyboru = new boolean[256];
        for (int i = 0; i < 256; i++) {
            JCheckBox poleWyboru = listaPolWyboru.get(i);
            if (poleWyboru.isSelected()) {
                stanPolaWyboru[i] = true;
            }
        }
        try {
            wyj.writeObject(uzytkownik + ": " + komunikatUzytkownika.getText());
            wyj.writeObject(stanPolaWyboru);
        } catch (IOException ex){
            System.out.println("Bardzo mi przykro! Nie udało się wysłać danych na serwer.");
            ex.printStackTrace();
        }
        komunikatUzytkownika.setText("");
    }

    public class WyborZListyListener implements ListSelectionListener {
        public void valueChanged(ListSelectionEvent lse) {
            if (lse.getValueIsAdjusting()) {
                String zaznaczony  = listaOtrzymanych.getSelectedValue();
                if (zaznaczony != null) {
                    boolean[] zaznaczonyStan = mapaOdebranychKompozycji.get(zaznaczony);
                    modyfikujSekwencje(zaznaczonyStan);
                    sekwenser.stop();
                    utworzSciezkeIOdtworz();
                }
            }
        }
    }

    private void modyfikujSekwencje(boolean[] stanPolaWyboru) {
        for (int i = 0; i < 256; i++) {
            JCheckBox poleWyboru = listaPolWyboru.get(i);
            poleWyboru.setSelected(stanPolaWyboru[i]);
        }
    }

    private void utworzSciezki(int[] lista) {
        for (int i = 0; i < 16; i++) {
            int klucz = lista[i];

            if (klucz != 0) {
                sciezka.add(tworzZdarzenie(NOTE_ON, 9, klucz, 100, i));
                sciezka.add(tworzZdarzenie(NOTE_OFF, 9, klucz, 100, i + 1));
            }
        }
    }

    public static MidiEvent tworzZdarzenie(int plc, int kanal, int jeden, int dwa, int takt){
        MidiEvent zdarzenie = null;
        try {
            ShortMessage msg = new ShortMessage();
            msg.setMessage(plc, kanal, jeden, dwa);
            zdarzenie = new MidiEvent(msg, takt);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return zdarzenie;
    }

    public class CzytelnikZdalnychDanych implements Runnable {
        public void run() {
            try{
                Object obj;
                while ((obj = wej.readObject()) != null) {
                    System.out.println("Pobrano obiekt z serwera");
                    System.out.println(obj.getClass());

                    String nazwaDoWyswietlenia = (String) obj;
                    boolean[] stanPolaWyboru = (boolean[]) wej.readObject();
                    mapaOdebranychKompozycji.put(nazwaDoWyswietlenia, stanPolaWyboru);

                    wektorLista.add(nazwaDoWyswietlenia);
                    listaOtrzymanych.setListData(wektorLista);
                }
            } catch (IOException | ClassNotFoundException ex) {
                ex.printStackTrace();
            }
        }
    }

    private void zapiszPlik() {
        boolean[] stanyPol = new boolean[256];

        for (int i = 0; i < 256; i++) {
            JCheckBox poleWyboru = listaPolWyboru.get(i);
            if (poleWyboru.isSelected()) {
                stanyPol[i] = true;
            }
        }
        try (ObjectOutputStream os =
                new ObjectOutputStream(new FileOutputStream("kompozycja.ser"))) {
            os.writeObject(stanyPol);
        } catch (IOException ex){
            ex.printStackTrace();
        }
    }

    private void wczytajPlik() {
        boolean[] stanyPol = null;
        try (ObjectInputStream is = new ObjectInputStream(new FileInputStream("kompozycja.ser"))) {
            stanyPol = (boolean[]) is.readObject();
        } catch (IOException | ClassNotFoundException ex) {}

        for (int i = 0; i < 256; i++) {
            JCheckBox poleWyboru = listaPolWyboru.get(i);
            poleWyboru.setSelected(stanyPol[i]);
        }

        sekwenser.stop();
        utworzSciezkeIOdtworz();
    }

}