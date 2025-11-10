package Servicios;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.UnknownHostException;

public class PopService {

     private final String SERVER = "mail.tecnoweb.org.bo";
     private final String USUARIO = "grupo03sc";
     private final String CONTRASEÑA = "grup012grup012*";

    private final int PUERTO = 110;

    public int getCantidadEmails() {
    String number = "";
    String comando;
    try {
        Socket socket = new Socket(SERVER, PUERTO);
        BufferedReader entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        DataOutputStream salida = new DataOutputStream(socket.getOutputStream());
        if (socket != null && entrada != null && salida != null) {

            // Leer el saludo inicial del servidor
            String respuesta = entrada.readLine();
            System.out.println("[POP-SERVICE] Respuesta inicial: " + respuesta);

            // Enviar comando USER
            comando = "USER " + USUARIO + "\r\n";
            salida.writeBytes(comando);
            respuesta = entrada.readLine(); // Lee la respuesta de USER
            System.out.println("[POP-SERVICE] Respuesta USER: " + respuesta);

            // Enviar comando PASS
            comando = "PASS " + CONTRASEÑA + "\r\n";
            salida.writeBytes(comando);
            respuesta = entrada.readLine(); // Lee la respuesta de PASS
            System.out.println("[POP-SERVICE] Respuesta PASS: " + respuesta);

            // Enviar comando LIST
            comando = "LIST\r\n";
            salida.writeBytes(comando);
            respuesta = entrada.readLine(); // Lee la respuesta de LIST
            System.out.println("[POP-SERVICE] Respuesta LIST: " + respuesta);

            // Verifica si la respuesta de LIST comienza con +OK
            if (respuesta.startsWith("+OK")) {
                // Divide la respuesta para obtener solo el número de emails
                String[] partes = respuesta.split(" ");
                if (partes.length > 1) {
                    number = partes[1]; // La cantidad de correos debería estar en la segunda posición
                }
            }
            
            int cantidadEmails = Integer.parseInt(number);
            System.out.println("[POP-SERVICE] Cantidad de correos: " + cantidadEmails);

            // Si hay más de 5 correos, eliminarlos
            /*if (cantidadEmails > 5) {
                System.out.println("[POP-SERVICE] Hay más de 5 correos, eliminando todos...");
                for (int i = 1; i <= cantidadEmails; i++) {
                    comando = "DELE " + i + "\r\n";
                    salida.writeBytes(comando);
                    respuesta = entrada.readLine();
                    System.out.println("[POP-SERVICE] Eliminando correo " + i + ": " + respuesta);
                }
            }*/

            // Enviar comando QUIT
            comando = "QUIT\r\n";
            salida.writeBytes(comando);

            salida.close();
            entrada.close();
            socket.close();
        }
    } catch (IOException e) {
        System.out.println("[POP-SERVICE] " + e);
    }

    // Convierte el número a entero, o devuelve 0 si no se pudo parsear
    try {
        return Integer.parseInt(number);
    } catch (NumberFormatException e) {
        System.out.println("[POP-SERVICE] Error al parsear la cantidad de emails: " + number);
        return 0;
    }
}



    public String getLastMail(BufferedReader in) throws IOException {
        String line;
        String lastLine = "";

        while (true) {
            line = in.readLine();
            if (line == null) {
                throw new IOException("S: Server cerró la conexión inesperadamente.");
            }
            if (line.equals(".")) {
                break; // Fin del mensaje
            }
            lastLine = line;
        }

        //System.out.println("[DEBUG] Última línea recibida: " + lastLine);

        if (lastLine.contains(" ")) {
            String[] partes = lastLine.split(" ");
            if (partes.length > 0) {
                String numero = partes[0].trim();
                //System.out.println("[DEBUG] Número de mensaje extraído: " + numero);
                return numero;
            }
        }

        System.out.println("[POP-SERVICE] Formato inesperado en la última línea recibida: " + lastLine);
        return null;
    }

   public String getMail() {
        String comando;
        String lastEmail;
        String email = "";
        try {
            Socket socket = new Socket(SERVER, PUERTO);
            BufferedReader entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            DataOutputStream salida = new DataOutputStream(socket.getOutputStream());

            if (socket != null && entrada != null && salida != null) {
                // Autenticación
                comando = "USER " + USUARIO + "\r\n";
                salida.writeBytes(comando);

                comando = "PASS " + CONTRASEÑA + "\r\n";
                salida.writeBytes(comando);

                // Obtener el último mensaje
                comando = "LIST\r\n";
                salida.writeBytes(comando);
                lastEmail = getLastMail(entrada);

                //System.out.println("[DEBUG] Último email encontrado: " + lastEmail);

                // Validar si se obtuvo un número válido
                if (lastEmail == null || lastEmail.isEmpty() || !lastEmail.matches("\\d+")) {
                    System.out.println("[POP-SERVICE] Error: Número de mensaje inválido");
                    return null;
                }

                // Obtener el contenido del último correo
                comando = "RETR " + lastEmail + "\r\n";
                salida.writeBytes(comando);
                email = getMultiline(entrada);

                // Procesar el email para extraer el "Subject"
                /*String subject = procesarEmail(email);
                if (subject != null) {
                    System.out.println("[DEBUG] Asunto del último correo: " + subject);
                } else {
                    System.out.println("[ERROR] No se pudo extraer el asunto del correo");
                }*/

                comando = "QUIT\r\n";
                salida.writeBytes(comando);

                salida.close();
                entrada.close();
                socket.close();

                return email;
            }
        } catch (UnknownHostException e) {
            System.out.println("[POP-SERVICE] Error de conexión: " + e);
        } catch (IOException e) {
            System.out.println("[POP-SERVICE] Error de E/S: " + e);
        }
        return null;
    }

    static protected String getMultiline(BufferedReader in) throws IOException {
        StringBuilder lines = new StringBuilder();
        while (true) {
            String line = in.readLine();
            if (line == null) {
                throw new IOException(" S : Server unawares closed the connection.");
            }
            if (line.equals(".")) {
                break;
            }
            if (line.length() > 0 && line.charAt(0) == '.') {
                line = line.substring(1);
            }
            lines.append("\n").append(line);
        }
        return lines.toString();
    }

    public String procesarEmail(String email) {
        try {
            // Imprimir el email completo para depuración
            System.out.println("[DEBUG] Contenido del email recibido:\n" + email);

            // Verifica si el contenido tiene el campo "Subject: "
            int beginIndex = email.indexOf("Subject: ");
            if (beginIndex == -1) {
                System.out.println("[ERROR] No se encontró 'Subject: ' en el email.");
                return null;
            }
            beginIndex += "Subject: ".length();
            int contextStart = Math.max(0, beginIndex - 50);
            int contextEnd = Math.min(email.length(), beginIndex + 50);
            System.out.println("[DEBUG] Contexto del email alrededor del 'Subject':\n" + email.substring(contextStart, contextEnd));


            // Verifica si hay una nueva línea después del "Subject: "
            int endIndex = email.indexOf("\n", beginIndex);
            if (endIndex == -1) {
                System.out.println("[ERROR] No se encontró el final del campo 'Subject'");
                return null;
            }

            // Extraer el asunto
            String subject = email.substring(beginIndex, endIndex).trim();
            System.out.println("[DEBUG] Asunto extraído: " + subject);

            return subject;
        } catch (Exception e) {
            System.out.println("Error al procesar el email: " + e.getMessage());
            return null;
        }
    }

}
