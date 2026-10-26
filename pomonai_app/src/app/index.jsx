import React, { useState, useEffect } from "react";
import {
  Text,
  TextInput,
  Pressable,
  View,
  StyleSheet,
  SafeAreaView,
  ScrollView,
  KeyboardAvoidingView,
  Platform,
} from "react-native";
import * as DocumentPicker from "expo-document-picker";

const API_URL_DUVIDA = "http://localhost:8080/duvida";
const API_URL_UPLOAD = "http://localhost:8080/documentos/upload";

const TEMPOS = {
  FOCO: 25 * 60,
  CURTA: 5 * 60,
  LONGA: 15 * 60,
};

function formatar(total) {
  const min = String(Math.floor(total / 60)).padStart(2, "0");
  const seg = String(total % 60).padStart(2, "0");
  return min + ":" + seg;
}

export default function Index() {
  const [duvida, setDuvida] = useState("");
  const [modo, setModo] = useState("FOCO");
  const [segundos, setSegundos] = useState(TEMPOS.FOCO);
  const [rodando, setRodando] = useState(false);
  const [ciclos, setCiclos] = useState(0);
  const [aviso, setAviso] = useState("");
  const [respostaIa, setRespostaIa] = useState("");

  const tipoPausa = modo === "FOCO" ? "NENHUMA" : modo;

  function mudarModo(novoModo) {
    setModo(novoModo);
    setSegundos(TEMPOS[novoModo]);
    setRodando(false);
  }

  useEffect(() => {
    if (!rodando) return;
    const id = setInterval(() => setSegundos((s) => (s > 0 ? s - 1 : 0)), 1000);
    return () => clearInterval(id);
  }, [rodando]);

  useEffect(() => {
    if (rodando && segundos === 0) {
      if (modo === "FOCO") {
        const total = ciclos + 1;
        setCiclos(total);
        setAviso("Foco concluído! Hora de descansar.");
        mudarModo(total % 4 === 0 ? "LONGA" : "CURTA");
      } else {
        setAviso("Pausa terminou! Vamos focar de novo.");
        mudarModo("FOCO");
      }
    }
  }, [segundos, rodando]);

  const enviarDuvida = async () => {
    if (duvida.trim() === "") {
      setAviso("Digite sua dúvida antes de enviar.");
      return;
    }

    setAviso("Analisando dúvida com IA...");
    setRespostaIa("");

    try {
      const response = await fetch(API_URL_DUVIDA, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({ texto: duvida, tipoPausa }),
      });

      if (!response.ok) throw new Error("Falha " + response.status);

      const data = await response.json();

      if (data.respostaIa) {
        setAviso("Dúvida respondida!");
        setRespostaIa(data.respostaIa);
      } else {
        setAviso(
          "A IA não conseguiu responder agora. Sua dúvida foi para o FAQ e o professor poderá responder."
        );
      }
      setDuvida("");
    } catch (error) {
      setAviso("Erro ao enviar dúvida.");
    }
  };

  const fazerUploadPdf = async () => {
    try {
      const resultado = await DocumentPicker.getDocumentAsync({
        type: "application/pdf",
        copyToCacheDirectory: true,
      });

      if (resultado.canceled) return;

      setAviso("Enviando material para a IA...");
      const arquivo = resultado.assets[0];

      const formData = new FormData();
      if (Platform.OS === "web") {
        const blob = arquivo.file ?? (await (await fetch(arquivo.uri)).blob());
        formData.append("arquivo", blob, arquivo.name);
      } else {
        formData.append("arquivo", {
          uri: arquivo.uri,
          name: arquivo.name,
          type: arquivo.mimeType || "application/pdf",
        });
      }

      // Não definir Content-Type: o próprio fetch gera o multipart com o boundary
      const response = await fetch(API_URL_UPLOAD, {
        method: "POST",
        body: formData,
      });

      if (response.ok) {
        setAviso("PDF processado! A IA já pode responder sobre ele.");
      } else {
        throw new Error("Erro no servidor");
      }
    } catch (error) {
      setAviso("Erro ao processar o PDF.");
    }
  };

  return (
    <SafeAreaView style={styles.container}>
      <KeyboardAvoidingView
        behavior={Platform.OS === "ios" ? "padding" : "height"}
        style={{ flex: 1, width: "100%" }}
      >
        <ScrollView contentContainerStyle={styles.scrollContent}>

          <View style={styles.header}>
            <Text style={styles.title}>Pomonai APP</Text>
            <Text style={styles.subtitle}>
              {modo === "FOCO" ? "🎯 Sessão de Foco" : "☕ Pausa " + modo.toLowerCase()}
            </Text>
          </View>

          <View style={styles.card}>
            <View style={styles.chipsRow}>
              <Pressable
                style={[styles.chip, modo === "FOCO" && styles.chipAtivo]}
                onPress={() => mudarModo("FOCO")}
              >
                <Text style={[styles.chipTexto, modo === "FOCO" && styles.chipTextoAtivo]}>
                  Foco 25m
                </Text>
              </Pressable>

              <Pressable
                style={[styles.chip, modo === "CURTA" && styles.chipAtivo]}
                onPress={() => mudarModo("CURTA")}
              >
                <Text style={[styles.chipTexto, modo === "CURTA" && styles.chipTextoAtivo]}>
                  Pausa 5m
                </Text>
              </Pressable>

              <Pressable
                style={[styles.chip, modo === "LONGA" && styles.chipAtivo]}
                onPress={() => mudarModo("LONGA")}
              >
                <Text style={[styles.chipTexto, modo === "LONGA" && styles.chipTextoAtivo]}>
                  Pausa 15m
                </Text>
              </Pressable>
            </View>

            <Text style={styles.relogio}>{formatar(segundos)}</Text>

            <View style={styles.cicloBadge}>
              <Text style={styles.ciclos}>Pomodoros completos: {ciclos}</Text>
            </View>

            <View style={styles.controlesRow}>
              <Pressable
                style={[styles.buttonPrimary, rodando && styles.buttonPausado]}
                onPress={() => setRodando(!rodando)}
              >
                <Text style={styles.buttonPrimaryText}>{rodando ? "Pausar" : "Iniciar"}</Text>
              </Pressable>

              <Pressable style={styles.buttonSecondary} onPress={() => mudarModo(modo)}>
                <Text style={styles.buttonSecondaryText}>Reiniciar</Text>
              </Pressable>
            </View>
          </View>

          <View style={styles.card}>
            <Text style={styles.cardTitle}>Material de Estudo</Text>
            <Text style={styles.cardDescription}>
              Envie um PDF para a IA estudar e tirar suas dúvidas.
            </Text>

            <Pressable style={styles.buttonUpload} onPress={fazerUploadPdf}>
              <Text style={styles.buttonUploadText}>📎 Anexar PDF</Text>
            </Pressable>
          </View>

          <View style={styles.card}>
            <Text style={styles.cardTitle}>Dúvidas do Estudo</Text>
            <Text style={styles.cardDescription}></Text>

            <TextInput
              style={styles.input}
              placeholder="Digite sua dúvida sobre o material..."
              placeholderTextColor="#8fa3d9"
              value={duvida}
              onChangeText={setDuvida}
              multiline
              numberOfLines={3}
            />

            <Pressable
              style={[styles.buttonAi, !duvida.trim() && styles.buttonDisabled]}
              onPress={enviarDuvida}
            >
              <Text style={styles.buttonAiText}>Enviar dúvida para IA</Text>
            </Pressable>

            {aviso !== "" && (
              <View style={styles.avisoBox}>
                <Text style={styles.avisoTexto}>{aviso}</Text>
              </View>
            )}

            {respostaIa !== "" && (
              <View style={styles.respostaBox}>
                <Text style={styles.respostaTitulo}>Resposta da IA:</Text>
                <Text style={styles.respostaTexto}>{respostaIa}</Text>
              </View>
            )}
          </View>

        </ScrollView>
      </KeyboardAvoidingView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: "#0a0d4c" },
  scrollContent: { paddingHorizontal: 20, paddingVertical: 24, alignItems: "center" },
  header: { alignItems: "center", marginBottom: 20 },
  title: { fontSize: 28, fontWeight: "800", color: "#ffffff", letterSpacing: 0.5 },
  subtitle: { fontSize: 16, color: "#4da3ff", fontWeight: "700", marginTop: 4 },
  card: { width: "100%", maxWidth: 420, backgroundColor: "#131862", borderRadius: 20, padding: 20, alignItems: "center", marginBottom: 18, shadowColor: "#000", shadowOffset: { width: 0, height: 4 }, shadowOpacity: 0.25, shadowRadius: 6, elevation: 4 },
  cardTitle: { fontSize: 18, fontWeight: "700", color: "#ffffff", alignSelf: "flex-start", marginBottom: 4 },
  cardDescription: { fontSize: 13, color: "#8fa3d9", alignSelf: "flex-start", marginBottom: 14 },
  chipsRow: { flexDirection: "row", backgroundColor: "#0a0d4c", borderRadius: 14, padding: 4, width: "100%", justifyContent: "space-between", marginBottom: 16 },
  chip: { flex: 1, paddingVertical: 10, borderRadius: 10, alignItems: "center" },
  chipAtivo: { backgroundColor: "#2196f3" },
  chipTexto: { color: "#8fa3d9", fontWeight: "600", fontSize: 13 },
  chipTextoAtivo: { color: "#ffffff", fontWeight: "700" },
  relogio: { fontSize: 64, fontWeight: "800", color: "#ffffff", fontVariant: ["tabular-nums"], marginVertical: 4 },
  cicloBadge: { backgroundColor: "rgba(77, 163, 255, 0.12)", paddingHorizontal: 12, paddingVertical: 6, borderRadius: 12, marginBottom: 20 },
  ciclos: { fontSize: 13, color: "#cfe6ff", fontWeight: "600" },
  controlesRow: { flexDirection: "row", width: "100%", gap: 12 },
  buttonPrimary: { flex: 2, backgroundColor: "#2196f3", paddingVertical: 14, borderRadius: 14, alignItems: "center", justifyContent: "center" },
  buttonPausado: { backgroundColor: "#e53e3e" },
  buttonPrimaryText: { color: "#ffffff", fontSize: 16, fontWeight: "700" },
  buttonSecondary: { flex: 1, backgroundColor: "transparent", borderWidth: 1.5, borderColor: "#4da3ff", paddingVertical: 14, borderRadius: 14, alignItems: "center", justifyContent: "center" },
  buttonSecondaryText: { color: "#cfe6ff", fontSize: 15, fontWeight: "600" },
  buttonUpload: { width: "100%", backgroundColor: "transparent", borderWidth: 1.5, borderColor: "#00b4d8", paddingVertical: 12, borderRadius: 12, alignItems: "center", justifyContent: "center", marginBottom: 8 },
  buttonUploadText: { color: "#00b4d8", fontSize: 15, fontWeight: "600" },
  input: { width: "100%", minHeight: 80, backgroundColor: "#0a0d4c", color: "#ffffff", fontSize: 15, borderRadius: 12, borderWidth: 1.5, borderColor: "#4da3ff", paddingHorizontal: 14, paddingVertical: 12, textAlignVertical: "top", marginBottom: 14 },
  buttonAi: { width: "100%", backgroundColor: "#00b4d8", paddingVertical: 14, borderRadius: 14, alignItems: "center", justifyContent: "center" },
  buttonDisabled: { opacity: 0.5 },
  buttonAiText: { color: "#0a0d4c", fontSize: 16, fontWeight: "800" },
  avisoBox: { marginTop: 14, padding: 12, backgroundColor: "rgba(77, 163, 255, 0.15)", borderRadius: 10, width: "100%", alignItems: "center" },
  avisoTexto: { color: "#cfe6ff", fontSize: 13, fontWeight: "600", textAlign: "center" },
  respostaBox: { marginTop: 14, padding: 16, backgroundColor: "#0a0d4c", borderRadius: 12, width: "100%", borderWidth: 1, borderColor: "#00b4d8" },
  respostaTitulo: { color: "#00b4d8", fontSize: 14, fontWeight: "bold", marginBottom: 8 },
  respostaTexto: { color: "#ffffff", fontSize: 14, lineHeight: 20 },
});
