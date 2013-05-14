dname="CN=Crosson David, OU=R&D, O=Reuse, L=Plouasne, S=Côtes d'armor, C=FR"
keystorefile=keystore
keystorepass=changeit
keyalias=exproxy
rm -f $keystorefile
keytool -genkey -validity 3650 -keystore $keystorefile -storepass $keystorepass -keypass $keystorepass -alias $keyalias -dname "$dname"
keytool -selfcert -validity 3650 -alias $keyalias -keystore $keystorefile -storepass $keystorepass -keypass $keystorepass
keytool -list -keystore $keystorefile -storepass $keystorepass -keypass $keystorepass

