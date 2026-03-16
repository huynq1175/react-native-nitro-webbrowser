import { NitroModules } from 'react-native-nitro-modules';
import { Platform } from 'react-native';
import type {
  BrowserResult,
  InAppBrowserOptions,
  NitroWebbrowser,
} from './NitroWebbrowser.nitro';

export {
  DismissButtonStyle,
  ModalPresentationStyle,
  ModalTransitionStyle,
} from './NitroWebbrowser.nitro';

export type {
  InAppBrowserOptions,
  BrowserResult,
  FormSheetSize,
  BrowserAnimations,
} from './NitroWebbrowser.nitro';

export type AuthSessionResult = BrowserResult;

const NitroWebbrowserModule =
  NitroModules.createHybridObject<NitroWebbrowser>('NitroWebbrowser');

async function open(
  url: string,
  options?: InAppBrowserOptions
): Promise<BrowserResult> {
  return NitroWebbrowserModule.open(url, options ?? {});
}

async function openAuth(
  url: string,
  redirectUrl: string,
  options?: InAppBrowserOptions
): Promise<AuthSessionResult> {
  return NitroWebbrowserModule.openAuth(url, redirectUrl, options ?? {});
}

function close(): void {
  NitroWebbrowserModule.close();
}

function closeAuth(): void {
  NitroWebbrowserModule.closeAuth();
}

async function isAvailable(): Promise<boolean> {
  return NitroWebbrowserModule.isAvailable();
}

async function warmup(): Promise<boolean> {
  if (Platform.OS === 'android') {
    return NitroWebbrowserModule.warmup();
  }
  return false;
}

function mayLaunchUrl(mostLikelyUrl: string, otherUrls: string[] = []): void {
  if (Platform.OS === 'android') {
    NitroWebbrowserModule.mayLaunchUrl(mostLikelyUrl, otherUrls);
  }
}

export const InAppBrowser = {
  open,
  openAuth,
  close,
  closeAuth,
  isAvailable,
  warmup,
  mayLaunchUrl,
};

export default InAppBrowser;
