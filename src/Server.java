import javafx.util.Pair;
import server.models.Course;
import server.models.RegistrationForm;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class Server {

    public final static String REGISTER_COMMAND = "INSCRIRE";
    public final static String LOAD_COMMAND = "CHARGER";
    private final ServerSocket server;
    private Socket client;
    private ObjectInputStream objectInputStream;
    private ObjectOutputStream objectOutputStream;
    private final ArrayList<EventHandler> handlers;

    /**
     Ce constructeur va créer une instance d'un serveur ainsi que le eventhandler qui lui sera associé.
     Le port sur lequel il est possible pour un client de se connecter sur le serveur.
     */

    public Server(int port) throws IOException {
        this.server = new ServerSocket(port, 1);
        this.handlers = new ArrayList<EventHandler>();
        this.addEventHandler(this::handleEvents);
    }

    /**
     Va ajouter un eventHandler.
     le eventHandler à ajouter.
     */
    public void addEventHandler(EventHandler h) {
        this.handlers.add(h);
    }

    /**
     TODO
     @param arg cmd et arg.
     */
    private void alertHandlers(String cmd, String arg) {
        for (EventHandler h : this.handlers) {
            h.handle(cmd, arg);
        }
    }

    /**
     La méthode qui va débuter le fonctionnement du serveur.
     */

    public void run() {
        while (true) {
            try {
                client = server.accept();
                System.out.println("Connecté au client: " + client);
                objectInputStream = new ObjectInputStream(client.getInputStream());
                objectOutputStream = new ObjectOutputStream(client.getOutputStream());
                listen();
                disconnect();
                System.out.println("Client déconnecté!");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     Fonction qui écoute les événement générés par le client.
     */

    public void listen() throws IOException, ClassNotFoundException {
        String line;
        if ((line = this.objectInputStream.readObject().toString()) != null) {
            Pair<String, String> parts = processCommandLine(line);
            String cmd = parts.getKey();
            String arg = parts.getValue();
            this.alertHandlers(cmd, arg);
        }
    }

    /**
     Méthode qui pair.
     pas clair encore
     */

    public Pair<String, String> processCommandLine(String line) {
        String[] parts = line.split(" ");
        String cmd = parts[0];
        String args = String.join(" ", Arrays.asList(parts).subList(1, parts.length));
        return new Pair<>(cmd, args);
    }

    /**
     Fonction qui permet de se déconnecter du serveur.
     */

    public void disconnect() throws IOException {
        objectOutputStream.close();
        objectInputStream.close();
        client.close();
    }

    /**
     Fonction qui handle les events.
     @param arg cmd et arg?
     */
    public void handleEvents(String cmd, String arg) {
        System.out.println(cmd);
        if (cmd.equals(REGISTER_COMMAND)) {
            handleRegistration();
        } else if (cmd.equals(LOAD_COMMAND)) {
            handleLoadCourses(arg);
        }
    }

    /**
     Lire un fichier texte contenant des informations sur les cours et les transofmer en liste d'objets 'Course'.
     La méthode filtre les cours par la session spécifiée en argument.
     Ensuite, elle renvoie la liste des cours pour une session au client en utilisant l'objet 'objectOutputStream'.
     La méthode gère les exceptions si une erreur se produit lors de la lecture du fichier ou de l'écriture de l'objet dans le flux.
     @param arg la session pour laquelle on veut récupérer la liste des cours
     */
    public void handleLoadCourses(String arg) {
        try {
            ArrayList<Course> course = new ArrayList<>();

            InputStream stream = getClass().getClassLoader().getResourceAsStream("data/cours.txt");
            BufferedReader in = new BufferedReader(new InputStreamReader(stream));

            String line;
            while ((line = in.readLine()) != null) {
                String[] ligne = line.split("\\s");
                String session = ligne[2];
                if (Objects.equals(session, arg)) {
                    Course temp = new Course(ligne[1], ligne[0], ligne[2]);
                    course.add(temp);
                }
            }
            objectOutputStream.writeObject(course);
            objectOutputStream.flush();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     Récupérer l'objet 'RegistrationForm' envoyé par le client en utilisant 'objectInputStream', l'enregistrer dans un fichier texte
     et renvoyer un message de confirmation au client.
     La méthode gére les exceptions si une erreur se produit lors de la lecture de l'objet, l'écriture dans un fichier ou dans le flux de sortie.
     */
    public void handleRegistration() {
        try {
            RegistrationForm fiche = (RegistrationForm) objectInputStream.readObject();
            String path = System.getProperty("user.home") + File.separator + "Desktop" + File.separator + "ServeurTest";
            File customPath = new File(path + File.separator + "inscription.txt");
            FileWriter myWriter = new FileWriter(customPath);
            myWriter.write(fiche.getCourse().getSession() + "\t" + fiche.getCourse().getCode() + "\t" +
                    fiche.getMatricule() + "\t" + fiche.getPrenom() + "\t" + fiche.getNom() + "\t" +
                    fiche.getEmail() + "\n");
            myWriter.close();
            System.out.println("Inscription ajouter au fichier txt.");
            objectOutputStream.writeObject("Félicitation! Inscription réussie de " + fiche.getPrenom() +
                    " au cours " + fiche.getCourse().getCode() + ".");
            objectOutputStream.flush();
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

    }
}