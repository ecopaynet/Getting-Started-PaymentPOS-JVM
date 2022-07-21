package com.company;

import com.ecopaynet.module.paymentpos.*;
import com.ecopaynet.module.paymentpos.Error;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Semaphore;

public class Main {
    public static void main(String[] args) {
        PaymentPOSDemo demo = new PaymentPOSDemo();
        demo.start(1);
    }
}

class PaymentPOSDemo implements Events.Initialization, Events.Transaction {
    private Semaphore semaphore = new Semaphore(1);
    private boolean isErrorOccurred = false;

    private TransactionResult transactionResult = null;

    public void start(int amount) {
        try {            
            semaphore.acquire();

            if(initialize()) {
                if(sale(amount)) {
                    refund(amount);
                }
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onInitializationComplete() {
        System.out.println("onInitializationComplete");
        semaphore.release();
    }

    @Override
    public void onInitializationError(Error error) {
        System.out.println("onInitializationError: " + error.getMessage());
        isErrorOccurred = true;
        semaphore.release();
    }

    @Override
    public void onTransactionComplete(TransactionResult transactionResult) {
        this.transactionResult = transactionResult;
        String commerceTicket = PaymentPOS.generateCommerceTransactionTicketText(transactionResult);
        System.out.println(commerceTicket);
        semaphore.release();
    }

    @Override
    public void onTransactionError(Error error) {
        isErrorOccurred = true;
        semaphore.release();
    }

    @Override
    public void onTransactionDisplayDCCMessage(String s) {
        System.out.println("MSG DCC: " + s);
    }

    @Override
    public void onTransactionDisplayMessage(String s) {
        System.out.println("MSG: " + s);
    }

    @Override
    public void onTransactionRequestSignature(TransactionRequestSignatureInformation transactionRequestSignatureInformation) {
        PaymentPOS.returnTransactionRequestedSignature(null);
    }

    private boolean initialize() throws InterruptedException {
        PaymentPOS.setEnvironment("TEST");
        
        PaymentPOS.addLogEventHandler((logLevel, message) -> {
            System.out.println("TRC: " + message);
        });

        Device device = new DeviceTcpip("192.168.1.239", 5556);
        //Device device = new DeviceSerial("COM3");

        if(PaymentPOS.initialize(device, this)) {
            System.out.println("Initializing...");
            semaphore.acquire();
            return !isErrorOccurred;
        } else {
            System.out.println("Initialization failed");
            return false;
        }
    }
    
    private boolean sale(int amount) throws InterruptedException {
        if(PaymentPOS.sale(amount, this)) {
            System.out.println("Performing sale...");
            semaphore.acquire();
            return !isErrorOccurred;
        } else {
            System.out.println("Sale failed");
            return false;
        }
    }

    private boolean refund(int amount) throws InterruptedException {
        if(PaymentPOS.refund(
            amount, 
            this.transactionResult.getOperationNumber(),
            this.transactionResult.getAuthorizationCode(),
            new kotlinx.datetime.LocalDate(java.time.LocalDate.now()),
            this
        )) {
            System.out.println("Performing refund...");
            semaphore.acquire();
            return !isErrorOccurred;
        } else {
            System.out.println("Refund failed");
            return false;
        }
    }
}