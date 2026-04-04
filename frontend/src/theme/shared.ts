/** 모든 테마가 공유하는 기본 토큰 */
export const sharedFontFamily =
  "'Pretendard Variable', Pretendard, -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, 'Noto Sans', 'Noto Sans KR', sans-serif";

export const sharedComponents = {
  Button: { controlHeight: 36, paddingContentHorizontal: 16 },
  Input: { controlHeight: 36 },
  Select: { controlHeight: 36 },
  Table: { fontSize: 14 },
  Card: { boxShadowTertiary: '0 1px 2px 0 rgba(0, 0, 0, 0.06)' },
  Menu: { itemBorderRadius: 6, itemMarginInline: 8 },
} as const;
