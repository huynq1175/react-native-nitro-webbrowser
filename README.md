# react-native-nitro-webbrowser

In-app browser for React Native, powered by [Nitro Modules](https://nitro.margelo.com/).

- **iOS**: `SFSafariViewController` + `ASWebAuthenticationSession`
- **Android**: Chrome Custom Tabs

## Installation

```sh
npm install react-native-nitro-webbrowser react-native-nitro-modules
```

> `react-native-nitro-modules` is a required peer dependency.

### iOS

```sh
cd ios && pod install
```

### Android

No additional setup required.

## Usage

### Open a URL in the in-app browser

```tsx
import InAppBrowser from 'react-native-nitro-webbrowser';

const result = await InAppBrowser.open('https://example.com', {
  // iOS
  dismissButtonStyle: 'done',
  preferredBarTintColor: '#453AA4',
  preferredControlTintColor: '#FFFFFF',
  readerMode: false,
  animated: true,
  modalEnabled: true,
  modalPresentationStyle: 'fullScreen',
  enableBarCollapsing: false,
  // Android
  showTitle: true,
  toolbarColor: '#453AA4',
  enableUrlBarHiding: true,
  enableDefaultShare: true,
  forceCloseOnRedirection: false,
  hasBackButton: true,
  showInRecents: false,
  includeReferrer: true,
});

// result.type: 'cancel' | 'dismiss'
```

### Authentication (OAuth)

```tsx
import InAppBrowser from 'react-native-nitro-webbrowser';

const result = await InAppBrowser.openAuth(
  'https://auth.example.com/authorize?client_id=xxx&redirect_uri=myapp://callback',
  'myapp://callback',
  {
    ephemeralWebSession: true, // iOS: don't share cookies with Safari
    // Android: show as bottom sheet (Partial Custom Tabs, Chrome 107+)
    bottomSheet: true,
    bottomSheetHeightRatio: 0.9,
    showTitle: true,
    toolbarColor: '#453AA4',
  }
);

if (result.type === 'success') {
  console.log('Redirect URL:', result.url);
} else {
  console.log('Cancelled');
}
```

On Android with `bottomSheet: true`, the auth browser appears as a bottom sheet overlay (similar to iOS `ASWebAuthenticationSession`). Share, bookmarks, and download buttons are automatically hidden for a clean auth experience. Falls back to full-screen if the browser doesn't support Partial Custom Tabs.

### Close the browser programmatically

```tsx
InAppBrowser.close();     // Close in-app browser
InAppBrowser.closeAuth(); // Close auth session
```

### Check availability

```tsx
const available = await InAppBrowser.isAvailable();
```

### Pre-warm the browser (Android only)

```tsx
// Bind to Chrome Custom Tabs service for faster launch
await InAppBrowser.warmup();

// Hint a likely URL for pre-fetching
InAppBrowser.mayLaunchUrl('https://example.com', [
  'https://example.com/about',
]);
```

## API

### `InAppBrowser.open(url, options?)`

Opens a URL in the in-app browser. Returns `Promise<BrowserResult>`.

### `InAppBrowser.openAuth(url, redirectUrl, options?)`

Opens an authentication session. Returns `Promise<BrowserResult>` with the redirect URL on success.

- **iOS**: Uses `ASWebAuthenticationSession` (native bottom sheet)
- **Android**: Uses Chrome Custom Tabs. Set `bottomSheet: true` for a bottom sheet presentation (Partial Custom Tabs, requires Chrome 107+). Automatically hides share, bookmarks, and download buttons for a minimal auth UI. Falls back to full-screen on unsupported browsers.

### `InAppBrowser.close()`

Dismisses the currently open in-app browser.

### `InAppBrowser.closeAuth()`

Cancels the current authentication session.

### `InAppBrowser.isAvailable()`

Returns `Promise<boolean>` indicating if the in-app browser is available.

### `InAppBrowser.warmup()`

Android only. Binds to the Chrome Custom Tabs service for faster browser launch. Returns `Promise<boolean>`.

### `InAppBrowser.mayLaunchUrl(mostLikelyUrl, otherUrls?)`

Android only. Hints the browser to pre-fetch the given URL.

## Options

### iOS Options

| Option | Type | Description |
|--------|------|-------------|
| `dismissButtonStyle` | `'done' \| 'close' \| 'cancel'` | Style of the dismiss button |
| `preferredBarTintColor` | `string` | Hex color for the toolbar background |
| `preferredControlTintColor` | `string` | Hex color for toolbar controls |
| `readerMode` | `boolean` | Enter Reader mode if available |
| `animated` | `boolean` | Animate presentation/dismissal (default: `true`) |
| `modalEnabled` | `boolean` | Present in a modal navigation controller |
| `modalPresentationStyle` | `string` | Modal presentation style |
| `modalTransitionStyle` | `string` | Modal transition animation |
| `enableBarCollapsing` | `boolean` | Allow toolbar to collapse on scroll |
| `ephemeralWebSession` | `boolean` | Don't share cookies with Safari (auth only) |
| `formSheetPreferredContentSize` | `{ width, height }` | Size for form sheet presentation |

### Android Options

| Option | Type | Description |
|--------|------|-------------|
| `showTitle` | `boolean` | Show the page title in the toolbar |
| `toolbarColor` | `string` | Hex color for the toolbar |
| `secondaryToolbarColor` | `string` | Hex color for the secondary toolbar |
| `navigationBarColor` | `string` | Hex color for the navigation bar |
| `navigationBarDividerColor` | `string` | Hex color for navigation bar divider |
| `enableUrlBarHiding` | `boolean` | Hide URL bar on scroll |
| `enableDefaultShare` | `boolean` | Add default share action |
| `forceCloseOnRedirection` | `boolean` | Close on external redirect |
| `hasBackButton` | `boolean` | Show a back arrow instead of X |
| `browserPackage` | `string` | Package name of the browser to use |
| `showInRecents` | `boolean` | Show in recent apps |
| `includeReferrer` | `boolean` | Include referrer header |
| `headers` | `Record<string, string>` | Custom HTTP headers |
| `animations` | `BrowserAnimations` | Custom enter/exit animations |
| `bottomSheet` | `boolean` | Show as bottom sheet (Partial Custom Tabs, auth only) |
| `bottomSheetHeightRatio` | `number` | Bottom sheet height ratio 0-1 (default: `0.7`) |

### BrowserResult

```typescript
interface BrowserResult {
  type: string;    // 'success' | 'cancel' | 'dismiss'
  message?: string;
  url?: string;    // Redirect URL (auth only)
}
```

## License

MIT
