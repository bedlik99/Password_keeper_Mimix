#!/bin/bash
SCRIPT=$(realpath -s "$0")
SCRIPTPATH=$(dirname "$SCRIPT")
TMP_PATH_FILE="tmp_path.txt"

echo "$SCRIPTPATH" | sed -e 's/^\///' -e 's/\//\\/g' -e 's/^./\0/' > "$TMP_PATH_FILE"

while IFS= read -r line; do
    SCRIPTPATH="${line^}\bin"
done < "$TMP_PATH_FILE"

if [ -f "$TMP_PATH_FILE" ]; then
    rm "$TMP_PATH_FILE" 
fi

if [ ${SCRIPTPATH:1:1} = '\' ]; then
   SCRIPTPATH=${SCRIPTPATH:0:1}:${SCRIPTPATH:1}
fi

java --module-path ${SCRIPTPATH} --add-modules javafx.controls,javafx.fxml,javafx.graphics,com.jfoenix,jbcrypt,json.simple,org.controlsfx.controls --add-opens=java.base/java.lang.reflect=com.jfoenix --add-exports javafx.graphics/com.sun.javafx.scene=com.jfoenix -jar Mimix.jar &