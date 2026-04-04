import type { AppTheme } from './types';
import atlassianBlue from './themes/atlassianBlue';
import tealGreen from './themes/tealGreen';
import violetPurple from './themes/violetPurple';
import sydneyCoastal from './themes/sydneyCoastal';
import rioDeJaneiro from './themes/rioDeJaneiro';
import sakura from './themes/sakura';

export const themes: AppTheme[] = [atlassianBlue, tealGreen, violetPurple, sydneyCoastal, rioDeJaneiro, sakura];

export const themeMap = new Map(themes.map((t) => [t.key, t]));

export const DEFAULT_THEME_KEY = 'atlassian-blue';

export function getTheme(key: string): AppTheme {
  return themeMap.get(key) ?? atlassianBlue;
}
