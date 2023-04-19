@FunctionalInterface


public interface EventHandler {

    /**
     * Cette interface fonctionelle définit la fonction abstraite handle.
     * @param cmd va prendre en paramètre la commande du client
     * @param arg va prendre en paramètre l'argument de la commande du client.
     */
    void handle(String cmd, String arg);
}
