import React, { useState } from 'react';
import { FlatList, Modal, Pressable, StyleSheet, Text, View } from 'react-native';
import { colors } from './theme';

export function PrimaryButton({
  title,
  onPress,
  disabled,
}: {
  title: string;
  onPress: () => void;
  disabled?: boolean;
}) {
  return (
    <Pressable
      onPress={onPress}
      disabled={disabled}
      style={({ pressed }) => [styles.btn, disabled && styles.btnDisabled, pressed && !disabled && styles.btnPressed]}
      accessibilityRole="button"
    >
      <Text style={styles.btnText}>{title}</Text>
    </Pressable>
  );
}

export function SectionHeader({ children }: { children: string }) {
  return <Text style={styles.section}>{children}</Text>;
}

export function Label({ children }: { children: string }) {
  return <Text style={styles.label}>{children}</Text>;
}

/**
 * Stand-in for Android's Spinner: a field that opens a modal list. Kept
 * dependency-free (no @react-native-picker) to minimise native surface.
 */
export function Choice({
  label,
  options,
  value,
  onChange,
  disabled,
}: {
  label: string;
  options: string[];
  value: string | null;
  onChange: (v: string) => void;
  disabled?: boolean;
}) {
  const [open, setOpen] = useState(false);
  return (
    <View style={styles.choiceWrap}>
      <Label>{label}</Label>
      <Pressable
        disabled={disabled || options.length === 0}
        onPress={() => setOpen(true)}
        style={[styles.field, (disabled || options.length === 0) && styles.fieldDisabled]}
        accessibilityRole="button"
      >
        <Text style={[styles.fieldText, !value && styles.placeholder]} numberOfLines={1}>
          {value ?? (options.length === 0 ? 'None available' : 'Choose…')}
        </Text>
        <Text style={styles.chevron}>▾</Text>
      </Pressable>
      <Modal visible={open} animationType="slide" transparent onRequestClose={() => setOpen(false)}>
        <Pressable style={styles.backdrop} onPress={() => setOpen(false)} />
        <View style={styles.sheet}>
          <Text style={styles.sheetTitle}>{label}</Text>
          <FlatList
            data={options}
            keyExtractor={(o) => o}
            renderItem={({ item }) => (
              <Pressable
                onPress={() => {
                  onChange(item);
                  setOpen(false);
                }}
                style={({ pressed }) => [styles.option, pressed && styles.optionPressed]}
              >
                <Text style={[styles.optionText, item === value && styles.optionSelected]}>{item}</Text>
                {item === value ? <Text style={styles.check}>✓</Text> : null}
              </Pressable>
            )}
          />
        </View>
      </Modal>
    </View>
  );
}

const styles = StyleSheet.create({
  btn: {
    backgroundColor: colors.accent,
    borderRadius: 10,
    paddingVertical: 14,
    alignItems: 'center',
  },
  btnPressed: { backgroundColor: colors.accentLight },
  btnDisabled: { opacity: 0.45 },
  btnText: { color: colors.btnText, fontSize: 16, fontWeight: '600' },
  section: {
    color: colors.subtext,
    fontSize: 12,
    fontWeight: '700',
    letterSpacing: 1,
    marginTop: 24,
    marginBottom: 8,
  },
  label: { color: colors.text, fontSize: 14, marginBottom: 6 },
  choiceWrap: { marginBottom: 14 },
  field: {
    backgroundColor: colors.fieldBg,
    borderColor: colors.fieldStroke,
    borderWidth: 1,
    borderRadius: 10,
    paddingHorizontal: 12,
    paddingVertical: 12,
    flexDirection: 'row',
    alignItems: 'center',
  },
  fieldDisabled: { opacity: 0.5 },
  fieldText: { flex: 1, color: colors.text, fontSize: 16 },
  placeholder: { color: colors.subtext },
  chevron: { color: colors.icon, fontSize: 16, marginLeft: 8 },
  backdrop: { flex: 1, backgroundColor: 'rgba(0,0,0,0.35)' },
  sheet: {
    backgroundColor: colors.cardBg,
    borderTopLeftRadius: 16,
    borderTopRightRadius: 16,
    maxHeight: '60%',
    paddingBottom: 24,
  },
  sheetTitle: {
    color: colors.subtext,
    fontSize: 13,
    fontWeight: '600',
    paddingHorizontal: 20,
    paddingVertical: 14,
  },
  option: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 20,
    paddingVertical: 14,
    borderTopColor: colors.fieldStroke,
    borderTopWidth: StyleSheet.hairlineWidth,
  },
  optionPressed: { backgroundColor: colors.fieldBg },
  optionText: { flex: 1, color: colors.text, fontSize: 16 },
  optionSelected: { color: colors.accent, fontWeight: '600' },
  check: { color: colors.accent, fontSize: 16 },
});
