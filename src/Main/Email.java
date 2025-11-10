package Main;

import Presentacion.Route;
import Servicios.GmailService;
import Servicios.PopService;
import Servicios.SmtpService;
import Utils.Auth;
import Utils.EmailHandler;
import Utils.Help;

public class Email {

    public static void main(String[] args) {
        PopService pop = new PopService();
        SmtpService smtp = new SmtpService();
        //GmailService smtp = new GmailService();

        Route route = new Route();
        System.out.println("Iniciando servidor...");

        int cantMails = pop.getCantidadEmails();
        System.out.println("Cantidad de emails: " + cantMails);

        while (true) {
            try {
                int newCantsMails = pop.getCantidadEmails();
                if (cantMails != newCantsMails) {
                    cantMails = newCantsMails;
                    System.out.println("***********************NEW EMAIL********************************");
                    try {
                        String email = pop.getMail();
                        try {                            
                            if (email == null) {
                                System.out.println("[ERROR] No se pudo obtener el correo: getMail() devolvió null.");
                            } else {
                                System.out.println("[DEBUG] Correo recibido exitosamente.");
                            }
                        } catch (Exception e) {
                            System.out.println("[ERROR] Excepción al obtener el correo: " + e.getMessage());
                        }                       
                        
                        EmailHandler emailHandler = new EmailHandler(email);
                        System.out.println("Email: " + emailHandler.remitente);
                        System.out.println("Subject: " + emailHandler.subject);

                        // Comparación sin distinción de mayúsculas/minúsculas
                        if ("HELP".equalsIgnoreCase(emailHandler.subject)) {
                            smtp.sendEmail(Help.getHelp(), emailHandler.remitente);
                            System.out.println("remitente: " + emailHandler.remitente);
                            System.out.println("******************************************************************");
                            continue;
                        }

                        if (!emailHandler.isValidate()) {
                            System.out.println("Error: " + emailHandler.messageError);
                            smtp.sendEmailError("Error", emailHandler.messageError, emailHandler.remitente);
                            System.out.println("******************************************************************");
                            continue;
                        }

                        String comando = emailHandler.getComando();
                        if (!Auth.auth(emailHandler.remitente) && !"ADDCLIENT".equalsIgnoreCase(comando)) {
                            smtp.sendEmailError("Error", "Usuario no autorizado", emailHandler.remitente);
                            System.out.println("******************************************************************");
                            continue;
                        }
                        String response = route.routes(emailHandler);
                        smtp.sendEmail(response, emailHandler.remitente);
                    } catch (Exception e) {
                        System.out.println("Error al procesar el email: " + e.getMessage());
                        e.printStackTrace();
                    }
                    System.out.println("******************************************************************");
                }
                Thread.sleep(5000); // Intervalo de 5 segundos
            } catch (InterruptedException ex) {
                System.out.println("Error en el servidor.");
                break;
            } catch (Exception e) {
                System.out.println("Error general en el servidor: " + e.getMessage());
            }
        }
    }

}

