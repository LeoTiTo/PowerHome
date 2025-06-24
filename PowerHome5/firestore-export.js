const admin = require('firebase-admin');
const fs = require('fs');
const path = require('path');

// Récupérer le chemin vers le fichier de clé de service depuis une variable d'environnement ou utiliser un chemin par défaut
const serviceAccountPath = process.env.SERVICE_ACCOUNT_PATH || './serviceAccountKey.json';

// Vérifier si le fichier de clé de service existe
if (!fs.existsSync(serviceAccountPath)) {
  console.error(`Erreur : Le fichier de clé de service n'existe pas à l'emplacement ${serviceAccountPath}`);
  console.error('Veuillez télécharger votre clé de service depuis la console Firebase :');
  console.error('1. Allez sur https://console.firebase.google.com/');
  console.error('2. Sélectionnez votre projet');
  console.error('3. Allez dans Paramètres du projet > Comptes de service');
  console.error('4. Cliquez sur "Générer une nouvelle clé privée"');
  console.error('5. Enregistrez le fichier JSON à l\'emplacement spécifié ou définissez SERVICE_ACCOUNT_PATH');
  process.exit(1);
}

// Dossier de destination pour l'export
const exportDir = process.env.EXPORT_DIR || './firestore-export';

// Créer le dossier d'export s'il n'existe pas
if (!fs.existsSync(exportDir)) {
  fs.mkdirSync(exportDir, { recursive: true });
}

// Initialiser Firebase Admin avec les identifiants
try {
  admin.initializeApp({
    credential: admin.credential.cert(require(serviceAccountPath))
  });
} catch (error) {
  console.error('Erreur lors de l\'initialisation de Firebase Admin :', error);
  process.exit(1);
}

const db = admin.firestore();

/**
 * Fonction principale pour exporter la base de données
 */
async function exportFirestore() {
  try {
    console.log('Démarrage de l\'export Firestore...');
    
    // Obtenir toutes les collections
    const collections = await db.listCollections();
    console.log(`${collections.length} collections trouvées`);
    
    if (collections.length === 0) {
      console.log('Aucune collection à exporter');
      process.exit(0);
    }
    
    // Date pour le nom du fichier
    const timestamp = new Date().toISOString().replace(/:/g, '-').replace(/\./g, '-');
    const mainExportFile = path.join(exportDir, `firestore-export-${timestamp}.json`);
    
    // Objet pour stocker toutes les données
    const allData = {};
    
    // Exporter chaque collection
    for (const collection of collections) {
      const collectionName = collection.id;
      console.log(`Export de la collection: ${collectionName}`);
      
      // Obtenir tous les documents de la collection
      const snapshot = await collection.get();
      
      if (snapshot.empty) {
        console.log(`La collection ${collectionName} est vide`);
        allData[collectionName] = [];
        continue;
      }
      
      // Stocker les données des documents
      const documents = [];
      
      // Pour chaque document
      for (const doc of snapshot.docs) {
        const docData = doc.data();
        
        // Convertir les références Firestore, Timestamp, etc. en valeurs utilisables
        const processedData = processFirestoreData(docData);
        
        // Ajouter l'ID du document
        documents.push({
          id: doc.id,
          ...processedData
        });
      }
      
      // Ajouter la collection à l'objet de données complet
      allData[collectionName] = documents;
      
      // Exporter également chaque collection dans un fichier séparé
      const collectionExportFile = path.join(exportDir, `${collectionName}-${timestamp}.json`);
      fs.writeFileSync(collectionExportFile, JSON.stringify(documents, null, 2));
      console.log(`Collection ${collectionName} exportée dans ${collectionExportFile}`);
    }
    
    // Écrire toutes les données dans un seul fichier
    fs.writeFileSync(mainExportFile, JSON.stringify(allData, null, 2));
    
    console.log(`Export complet terminé! Fichier: ${mainExportFile}`);
    
  } catch (error) {
    console.error('Erreur lors de l\'export :', error);
    process.exit(1);
  }
}

/**
 * Traite les types de données Firestore spéciaux pour les rendre JSON-compatibles
 * @param {Object} data - Données à traiter
 * @return {Object} - Données traitées
 */
function processFirestoreData(data) {
  if (!data) return data;
  
  if (Array.isArray(data)) {
    return data.map(item => processFirestoreData(item));
  }
  
  if (typeof data === 'object') {
    // Traiter les timestamps
    if (data.constructor.name === 'Timestamp') {
      return {
        _type: 'timestamp',
        seconds: data.seconds,
        nanoseconds: data.nanoseconds,
        isoString: new Date(data.seconds * 1000).toISOString()
      };
    }
    
    // Traiter les références
    if (data.constructor.name === 'DocumentReference') {
      return {
        _type: 'reference',
        path: data.path
      };
    }
    
    // Traiter les géopoints
    if (data.constructor.name === 'GeoPoint') {
      return {
        _type: 'geopoint',
        latitude: data.latitude,
        longitude: data.longitude
      };
    }
    
    // Traiter les objets normaux
    const result = {};
    for (const key in data) {
      result[key] = processFirestoreData(data[key]);
    }
    return result;
  }
  
  // Retourner les valeurs primitives telles quelles
  return data;
}

// Exécuter l'export
exportFirestore().then(() => {
  console.log('Script terminé avec succès');
  process.exit(0);
}).catch(error => {
  console.error('Erreur dans le script principal :', error);
  process.exit(1);
}); 