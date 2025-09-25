const fs = require('fs');
const path = require('path');

const apiUrl = process.env.API_URL || 'http://localhost:8080';
const localUrlPrefix = 'http://localhost:'; 

const directoryToWalk = path.join(__dirname, 'app'); 

function replaceInFile(filePath) {
    try {
        let content = fs.readFileSync(filePath, 'utf8');
        
        const regexLocalhostWithPort = new RegExp(`${localUrlPrefix}\\d+`, 'g');
        
        let newContent = content.replace(regexLocalhostWithPort, apiUrl);

        newContent = newContent.replace(/http:\/\/localhost\//g, `${apiUrl}/`);

        if (newContent !== content) {
            console.log(`Substituindo URL em: ${filePath}`);
            fs.writeFileSync(filePath, newContent, 'utf8');
        }
    } catch (err) {
        if (err.code !== 'EISDIR' && err.code !== 'ENOENT') {
            console.error(`Erro ao processar ${filePath}: ${err.message}`);
        }
    }
}

function walkDir(dir) {
    fs.readdirSync(dir).forEach(file => {
        const filePath = path.join(dir, file);
        const stat = fs.statSync(filePath);
        
        if (stat.isDirectory()) {
            if (file !== 'node_modules' && file !== 'environments') {
                walkDir(filePath);
            }
        } else if (file.endsWith('.ts') || file.endsWith('.html') || file.endsWith('.js')) {
            replaceInFile(filePath);
        }
    });
}

if (fs.existsSync(directoryToWalk)) {
    walkDir(directoryToWalk);
    console.log('Correcao de URL da API concluida.');
} else {
    console.error(`Diretorio nao encontrado: ${directoryToWalk}`);
}
