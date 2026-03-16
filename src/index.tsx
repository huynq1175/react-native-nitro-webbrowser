import { NitroModules } from 'react-native-nitro-modules';
import type { NitroWebbrowser } from './NitroWebbrowser.nitro';

const NitroWebbrowserHybridObject =
  NitroModules.createHybridObject<NitroWebbrowser>('NitroWebbrowser');

export function multiply(a: number, b: number): number {
  return NitroWebbrowserHybridObject.multiply(a, b);
}
