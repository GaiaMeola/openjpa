#!/bin/bash
echo "--- Esecuzione Test e Generazione Report ---"

#!/bin/bash
# Attiva il profilo 'coverage', esegue i test di ClassUtil e genera il report
mvn clean verify -Pcoverage -Dtest=ClassUtil*

echo "--- Controlla ora in target/site/jacoco/index.html ---"