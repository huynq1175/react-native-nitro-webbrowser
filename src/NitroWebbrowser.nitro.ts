import type { HybridObject } from 'react-native-nitro-modules';

export enum DismissButtonStyle {
  DONE = 'done',
  CLOSE = 'close',
  CANCEL = 'cancel',
}

export enum ModalPresentationStyle {
  AUTOMATIC = 'automatic',
  FULL_SCREEN = 'fullScreen',
  PAGE_SHEET = 'pageSheet',
  FORM_SHEET = 'formSheet',
  CURRENT_CONTEXT = 'currentContext',
  CUSTOM = 'custom',
  OVER_FULL_SCREEN = 'overFullScreen',
  OVER_CURRENT_CONTEXT = 'overCurrentContext',
  POPOVER = 'popover',
  NONE = 'none',
}

export enum ModalTransitionStyle {
  COVER_VERTICAL = 'coverVertical',
  FLIP_HORIZONTAL = 'flipHorizontal',
  CROSS_DISSOLVE = 'crossDissolve',
  PARTIAL_CURL = 'partialCurl',
}

export interface FormSheetSize {
  width: number;
  height: number;
}

export interface BrowserAnimations {
  startEnter: string;
  startExit: string;
  endEnter: string;
  endExit: string;
}

export interface InAppBrowserOptions {
  // iOS options
  dismissButtonStyle?: DismissButtonStyle;
  preferredBarTintColor?: string;
  preferredControlTintColor?: string;
  readerMode?: boolean;
  animated?: boolean;
  modalPresentationStyle?: ModalPresentationStyle;
  modalTransitionStyle?: ModalTransitionStyle;
  modalEnabled?: boolean;
  enableBarCollapsing?: boolean;
  ephemeralWebSession?: boolean;
  formSheetPreferredContentSize?: FormSheetSize;

  // Android options
  showTitle?: boolean;
  toolbarColor?: string;
  secondaryToolbarColor?: string;
  navigationBarColor?: string;
  navigationBarDividerColor?: string;
  enableUrlBarHiding?: boolean;
  enableDefaultShare?: boolean;
  forceCloseOnRedirection?: boolean;
  animations?: BrowserAnimations;
  headers?: Record<string, string>;
  hasBackButton?: boolean;
  browserPackage?: string;
  showInRecents?: boolean;
  includeReferrer?: boolean;

  // Android bottom sheet options (Partial Custom Tabs)
  bottomSheet?: boolean;
}

export interface BrowserResult {
  type: string;
  message?: string;
  url?: string;
}

export interface NitroWebbrowser
  extends HybridObject<{ ios: 'swift'; android: 'kotlin' }> {
  open(url: string, options: InAppBrowserOptions): Promise<BrowserResult>;
  close(): void;
  openAuth(
    url: string,
    redirectUrl: string,
    options: InAppBrowserOptions
  ): Promise<BrowserResult>;
  closeAuth(): void;
  isAvailable(): Promise<boolean>;
  warmup(): Promise<boolean>;
  mayLaunchUrl(mostLikelyUrl: string, otherUrls: string[]): void;
}
