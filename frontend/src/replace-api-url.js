const fs = require('fs');
const path = require('path');

const apiUrl = 'https://agendador-de-processos.onrender.com';
const localUrl = 'http://localhost:';

const directory = path.join(__dirname, 'app');

function replaceInFile(filePath) {
    try {
        let content = fs.readFileSync(filePath, 'utf8');
        
        const regexLocalhost = new RegExp(`${localUrl}\\d+`, 'g');
        
        if (content.match(regexLocalhost)) {
            console.log(`Substituindo URL em: ${filePath}`);
            content = content.replace(regexLocalhost, apiUrl);
            fs.writeFileSync(filePath, content, 'utf8');
        }
    } catch (err) {
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
        } else if (file.endsWith('.ts')) {
            replaceInFile(filePath);
        }
    });
}

walkDir(directory);
console.log('Correcao de URL da API concluida.');
