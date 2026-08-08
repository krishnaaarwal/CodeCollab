import type { SupportedLanguage } from '@/types/api';

export interface LanguageBoilerplate {
  id: SupportedLanguage;
  label: string;
  fileName: string;
  /** Language id understood by Monaco (used from Phase 2 onward). */
  monacoLanguage: string;
  content: string;
}

export const LANGUAGE_BOILERPLATES: LanguageBoilerplate[] = [
  {
    id: 'JAVA',
    label: 'Java',
    fileName: 'Main.java',
    monacoLanguage: 'java',
    content: `public class Main {
    public static void main(String[] args) {
        System.out.println("Hello, Nexis!");
    }
}
`,
  },
  {
    id: 'PYTHON',
    label: 'Python',
    fileName: 'main.py',
    monacoLanguage: 'python',
    content: `def main():
    print("Hello, Nexis!")


if __name__ == "__main__":
    main()
`,
  },
  {
    id: 'JAVASCRIPT',
    label: 'JavaScript',
    fileName: 'index.js',
    monacoLanguage: 'javascript',
    content: `function main() {
  console.log("Hello, Nexis!");
}

main();
`,
  },
  {
    id: 'CPP',
    label: 'C++',
    fileName: 'main.cpp',
    monacoLanguage: 'cpp',
    content: `#include <iostream>

int main() {
    std::cout << "Hello, Nexis!" << std::endl;
    return 0;
}
`,
  },
  {
    id: 'DART',
    label: 'Dart',
    fileName: 'main.dart',
    monacoLanguage: 'dart',
    content: `void main() {
  print('Hello, Nexis!');
}
`,
  },
];

export function getBoilerplate(language: SupportedLanguage): LanguageBoilerplate {
  const found = LANGUAGE_BOILERPLATES.find((b) => b.id === language);
  if (!found) throw new Error(`No boilerplate registered for ${language}`);
  return found;
}

interface LanguageInfo {
  monacoLanguage: string;
  /** null when the extension isn't one the execution service supports. */
  executionLanguage: SupportedLanguage | null;
}

const EXTENSION_MAP: Record<string, LanguageInfo> = {
  java: { monacoLanguage: 'java', executionLanguage: 'JAVA' },
  py: { monacoLanguage: 'python', executionLanguage: 'PYTHON' },
  js: { monacoLanguage: 'javascript', executionLanguage: 'JAVASCRIPT' },
  jsx: { monacoLanguage: 'javascript', executionLanguage: 'JAVASCRIPT' },
  ts: { monacoLanguage: 'typescript', executionLanguage: null },
  tsx: { monacoLanguage: 'typescript', executionLanguage: null },
  cpp: { monacoLanguage: 'cpp', executionLanguage: 'CPP' },
  cc: { monacoLanguage: 'cpp', executionLanguage: 'CPP' },
  h: { monacoLanguage: 'cpp', executionLanguage: 'CPP' },
  dart: { monacoLanguage: 'dart', executionLanguage: 'DART' },
  json: { monacoLanguage: 'json', executionLanguage: null },
  md: { monacoLanguage: 'markdown', executionLanguage: null },
  txt: { monacoLanguage: 'plaintext', executionLanguage: null },
};

export function detectLanguageFromFileName(fileName: string): LanguageInfo {
  const ext = fileName.split('.').pop()?.toLowerCase() || '';
  return EXTENSION_MAP[ext] || { monacoLanguage: 'plaintext', executionLanguage: null };
}
