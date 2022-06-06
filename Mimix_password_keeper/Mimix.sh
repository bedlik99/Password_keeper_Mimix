#!/bin/bash
java --module-path "D:\ProgramFiles\Mimix_password_keeper\bin" --add-modules javafx.controls,javafx.fxml,javafx.graphics,com.jfoenix,jbcrypt,json.simple,org.controlsfx.controls --add-opens=java.base/java.lang.reflect=com.jfoenix --add-exports javafx.graphics/com.sun.javafx.scene=com.jfoenix -jar Mimix.jar &
