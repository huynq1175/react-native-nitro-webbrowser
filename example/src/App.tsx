import { useState } from 'react';
import {
  Alert,
  Button,
  ScrollView,
  StyleSheet,
  Text,
  View,
} from 'react-native';
import {
  DismissButtonStyle,
  InAppBrowser,
  ModalPresentationStyle,
} from 'react-native-nitro-webbrowser';

export default function App() {
  const [result, setResult] = useState<string>('');

  const openBrowser = async () => {
    try {
      const res = await InAppBrowser.open('https://reactnative.dev', {
        dismissButtonStyle: DismissButtonStyle.CLOSE,
        preferredBarTintColor: '#453AA4',
        preferredControlTintColor: '#FFFFFF',
        readerMode: false,
        animated: true,
        modalPresentationStyle: ModalPresentationStyle.FULL_SCREEN,
        modalEnabled: true,
        enableBarCollapsing: false,
        // Android
        showTitle: true,
        toolbarColor: '#453AA4',
        enableUrlBarHiding: true,
        enableDefaultShare: true,
        forceCloseOnRedirection: false,
        showInRecents: true,
      });
      setResult(JSON.stringify(res, null, 2));
    } catch (error) {
      setResult(`Error: ${error}`);
    }
  };

  const checkAvailability = async () => {
    try {
      const available = await InAppBrowser.isAvailable();
      Alert.alert('Availability', `InAppBrowser available: ${available}`);
    } catch (error) {
      Alert.alert('Error', `${error}`);
    }
  };

  const openAuth = async () => {
    try {
      const res = await InAppBrowser.openAuth(
        'https://accounts.google.com/o/oauth2/v2/auth?client_id=YOUR_CLIENT_ID&redirect_uri=YOUR_REDIRECT_URI&response_type=code&scope=email',
        'YOUR_REDIRECT_URI://',
        {
          ephemeralWebSession: true,
          showTitle: true,
          enableUrlBarHiding: true,
        }
      );
      setResult(JSON.stringify(res, null, 2));
    } catch (error) {
      setResult(`Error: ${error}`);
    }
  };

  const openAuthBottomSheet = async () => {
    try {
      const res = await InAppBrowser.openAuth(
        'https://study4.com/login/',
        'YOUR_REDIRECT_URI://',
        {
          hasBackButton: false,
          readerMode: false,
          ephemeralWebSession: true,
          showTitle: true,
          toolbarColor: '#453AA4',
          // Android: show as bottom sheet (Partial Custom Tabs)
          bottomSheet: true,
          bottomSheetHeightRatio: 0.9,
        }
      );
      setResult(JSON.stringify(res, null, 2));
    } catch (error) {
      setResult(`Error: ${error}`);
    }
  };

  return (
    <ScrollView contentContainerStyle={styles.container}>
      <Text style={styles.title}>NitroWebbrowser Example</Text>

      <View style={styles.buttonContainer}>
        <Button title="Open Browser" onPress={openBrowser} />
      </View>

      <View style={styles.buttonContainer}>
        <Button title="Check Availability" onPress={checkAvailability} />
      </View>

      <View style={styles.buttonContainer}>
        <Button title="Open Auth (Demo)" onPress={openAuth} />
      </View>

      <View style={styles.buttonContainer}>
        <Button
          title="Open Auth Bottom Sheet (Android)"
          onPress={openAuthBottomSheet}
        />
      </View>

      {result ? (
        <View style={styles.resultContainer}>
          <Text style={styles.resultTitle}>Result:</Text>
          <Text style={styles.resultText}>{result}</Text>
        </View>
      ) : null}
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: {
    flexGrow: 1,
    alignItems: 'center',
    justifyContent: 'center',
    padding: 20,
  },
  title: {
    fontSize: 24,
    fontWeight: 'bold',
    marginBottom: 30,
  },
  buttonContainer: {
    marginVertical: 8,
    width: '100%',
  },
  resultContainer: {
    marginTop: 20,
    padding: 16,
    backgroundColor: '#f0f0f0',
    borderRadius: 8,
    width: '100%',
  },
  resultTitle: {
    fontWeight: 'bold',
    marginBottom: 8,
  },
  resultText: {
    fontFamily: 'monospace',
  },
});
