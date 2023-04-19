public class ServerLauncher {
    public final static int PORT = 1337;

    /**
     * La classe Main va créer une intance du serveur en utilisant le port 1337 définit plus haut. Il va ensuite
     * envoyer une confirmation écrite si la connection est réussie et appeler la fonction run() pour démarrer les
     * tâches du serveur.
     * @param args
     */
    public static void main(String[] args) {
        Server server;
        try {
            server = new Server(PORT);
            System.out.println("Server is running...");
            server.run();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}