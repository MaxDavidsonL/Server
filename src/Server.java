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
     @param port Le port sur lequel il est possible pour un client de se connecter sur le serveur.
     */

    public Server(int port) throws IOException {
        this.server = new ServerSocket(port, 1);
        this.handlers = new ArrayList<EventHandler>();
        this.addEventHandler(this::handleEvents);
    }

    /**
     Va ajouter au ArrayList Handlers un EventHandler pris en paramètre.
     @param h Ce paramètre est le EventHandler à ajouter.
     */
    public void addEventHandler(EventHandler h) {
        this.handlers.add(h);
    }

    /**
     Cette fonction va attribuer les valeurs de cmd et de arg à chaque handle des membres du ArrayList EventHandler.
     @param cmd Ce paramètre est la commande recu de la fonction listen().
     @param arg Ce paramètre est l'argument suivant la commande recu de la fonction listen().
     */
    private void alertHandlers(String cmd, String arg) {
        for (EventHandler h : this.handlers) {
            h.handle(cmd, arg);
        }
    }

    /**
     La méthode qui va débuter le fonctionnement du serveur en acceptant la connection avec le client et en créant un
     ObjectInputStream et ObjectOutputStream prèts à recevoir ou à envoyer de l'information au Client. Il va appeler
     la fonction listen() jusqu'à la réception d'une commande puis va se déconnecter en appelant la fonction
     disconnect().
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
     Fonction qui qui va recevoir une ligne de texte du client, puis qui va la transformer en pair de deux String
     dont il va attribuer la valeur respectivement aux variables cmd et arg
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
     Cette méthode permet de séparer la ligne de texte reçu par le client en une paire d'éléments cmd et arg.
     @param line la ligne de texte reçu du client et créée par la fonction listen() qui va être séparéé en paire.
     */

    public Pair<String, String> processCommandLine(String line) {
        String[] parts = line.split(" ");
        String cmd = parts[0];
        String args = String.join(" ", Arrays.asList(parts).subList(1, parts.length));
        return new Pair<>(cmd, args);
    }

    /**
     Fonction qui permet de déconnecter le client du serveur.
     */

    public void disconnect() throws IOException {
        objectOutputStream.close();
        objectInputStream.close();
        client.close();
    }

    /**
     Cette fonction va prendre les valeurs de cmd et arg recues des autres fonctions et va finalement
     appeler la fonction et l'argument approprié.
     @param cmd la commande qui va déterminer la fonction à appeler.
     @param arg l'argument qui va indiquer la session utilisée par HandleLoadCourses
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
     Récupérer l'objet 'RegistrationForm' envoyé par le client en utilisant 'objectInputStream', l'enregistrer dans
     un fichier texte et renvoyer un message de confirmation au client. La méthode gére les exceptions si une erreur
     se produit lors de la lecture de l'objet, l'écriture dans un fichier ou dans le flux de sortie.
     */
    public void handleRegistration() {
        try {
            RegistrationForm fiche = (RegistrationForm) objectInputStream.readObject();
            String path = System.getProperty("user.home") + File.separator + "Desktop" + File.separator + "Serveur";
            File customPath = new File(path + File.separator + "inscription.txt");
            //FileWriter writer = new FileWriter(customPath);
            PrintWriter writer = new PrintWriter(new FileWriter(customPath, true));
            writer.write(fiche.getCourse().getSession() + "\t" + fiche.getCourse().getCode() + "\t" +
                    fiche.getMatricule() + "\t" + fiche.getPrenom() + "\t" + fiche.getNom() + "\t" +
                    fiche.getEmail() + "\n");
            writer.close();
            System.out.println("Inscription ajouter au fichier txt.");
            objectOutputStream.writeObject("Félicitation! Inscription réussie de " + fiche.getPrenom() +
                    " au cours " + fiche.getCourse().getCode() + ".");
            objectOutputStream.flush();
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

    }
}