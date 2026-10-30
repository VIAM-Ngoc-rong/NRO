@echo off
chcp 65001 >nul
title Bo Chuyen Doi Phim Di Chuyen WASD - Ngoc Rong VIAM
powershell -NoProfile -ExecutionPolicy Bypass -Command ^
"$code = @'
using System;
using System.Diagnostics;
using System.Runtime.InteropServices;
using System.Text;
using System.Windows.Forms;

public class WasdMapper {
    private const int WH_KEYBOARD_LL = 13;
    private const int WM_KEYDOWN = 0x0100;
    private const int WM_KEYUP = 0x0101;
    private const int WM_SYSKEYDOWN = 0x0104;
    private const int WM_SYSKEYUP = 0x0105;

    private const uint LLKHF_INJECTED = 0x00000010;
    private const uint KEYEVENTF_EXTENDEDKEY = 0x0001;
    private const uint KEYEVENTF_KEYUP = 0x0002;

    private const int VK_W = 0x57;
    private const int VK_A = 0x41;
    private const int VK_S = 0x53;
    private const int VK_D = 0x44;
    private const int VK_F12 = 0x7B;

    private const byte VK_LEFT = 0x25;
    private const byte VK_UP = 0x26;
    private const byte VK_RIGHT = 0x27;
    private const byte VK_DOWN = 0x28;

    private static IntPtr _hookID = IntPtr.Zero;
    private static LowLevelKeyboardProc _proc = HookCallback;
    private static bool _enabled = true;

    public delegate IntPtr LowLevelKeyboardProc(int nCode, IntPtr wParam, IntPtr lParam);

    [StructLayout(LayoutKind.Sequential)]
    private struct KBDLLHOOKSTRUCT {
        public uint vkCode;
        public uint scanCode;
        public uint flags;
        public uint time;
        public IntPtr dwExtraInfo;
    }

    [DllImport(\"user32.dll\", CharSet = CharSet.Auto, SetLastError = true)]
    private static extern IntPtr SetWindowsHookEx(int idHook, LowLevelKeyboardProc lpfn, IntPtr hMod, uint dwThreadId);

    [DllImport(\"user32.dll\", CharSet = CharSet.Auto, SetLastError = true)]
    [return: MarshalAs(UnmanagedType.Bool)]
    private static extern bool UnhookWindowsHookEx(IntPtr hhk);

    [DllImport(\"user32.dll\", CharSet = CharSet.Auto, SetLastError = true)]
    private static extern IntPtr CallNextHookEx(IntPtr hhk, int nCode, IntPtr wParam, IntPtr lParam);

    [DllImport(\"kernel32.dll\", CharSet = CharSet.Auto, SetLastError = true)]
    private static extern IntPtr GetModuleHandle(string lpModuleName);

    [DllImport(\"user32.dll\")]
    private static extern IntPtr GetForegroundWindow();

    [DllImport(\"user32.dll\")]
    private static extern uint GetWindowThreadProcessId(IntPtr hWnd, out uint lpdwProcessId);

    [DllImport(\"user32.dll\")]
    private static extern void keybd_event(byte bVk, byte bScan, uint dwFlags, UIntPtr dwExtraInfo);

    [DllImport(\"user32.dll\")]
    private static extern uint MapVirtualKey(uint uCode, uint uMapType);

    [DllImport(\"user32.dll\")]
    private static extern short GetAsyncKeyState(int vKey);

    private static bool IsGameActive() {
        IntPtr hwnd = GetForegroundWindow();
        if (hwnd == IntPtr.Zero) return false;
        uint pid;
        GetWindowThreadProcessId(hwnd, out pid);
        if (pid == 0) return false;
        try {
            Process p = Process.GetProcessById((int)pid);
            string name = p.ProcessName.ToLowerInvariant();
            return name.Contains(\"dragonboy\") || name.Contains(\"nro\") || name.Contains(\"ngocrong\") || name.Contains(\"xunglord\");
        } catch {
            return false;
        }
    }

    private static void SendArrow(byte vkArrow, bool isKeyUp) {
        byte scan = (byte)MapVirtualKey(vkArrow, 0);
        uint flags = KEYEVENTF_EXTENDEDKEY | (isKeyUp ? KEYEVENTF_KEYUP : 0);
        keybd_event(vkArrow, scan, flags, UIntPtr.Zero);
    }

    private static IntPtr HookCallback(int nCode, IntPtr wParam, IntPtr lParam) {
        if (nCode >= 0) {
            KBDLLHOOKSTRUCT info = (KBDLLHOOKSTRUCT)Marshal.PtrToStructure(lParam, typeof(KBDLLHOOKSTRUCT));
            if ((info.flags & LLKHF_INJECTED) == 0 && IsGameActive()) {
                int msg = wParam.ToInt32();
                bool isDown = (msg == WM_KEYDOWN || msg == WM_SYSKEYDOWN);
                bool isUp = (msg == WM_KEYUP || msg == WM_SYSKEYUP);

                if (isDown && info.vkCode == VK_F12) {
                    _enabled = !_enabled;
                    Console.ForegroundColor = _enabled ? ConsoleColor.Green : ConsoleColor.Yellow;
                    Console.WriteLine(_enabled ? \"[ON]  Da BAT di chuyen WASD trong game.\" : \"[OFF] Da TAM TAT WASD (de chat/go chu).\");
                    Console.Beep(_enabled ? 900 : 450, 120);
                    return (IntPtr)1;
                }

                // Khong chuyen doi khi dang giu Ctrl hoac Alt (ví dụ Ctrl+C, Ctrl+V)
                bool ctrlOrAlt = (GetAsyncKeyState(0x11) & 0x8000) != 0 || (GetAsyncKeyState(0x12) & 0x8000) != 0;

                if (_enabled && !ctrlOrAlt && (isDown || isUp)) {
                    switch ((int)info.vkCode) {
                        case VK_W:
                            SendArrow(VK_UP, isUp);
                            return (IntPtr)1;
                        case VK_A:
                            SendArrow(VK_LEFT, isUp);
                            return (IntPtr)1;
                        case VK_S:
                            SendArrow(VK_DOWN, isUp);
                            return (IntPtr)1;
                        case VK_D:
                            SendArrow(VK_RIGHT, isUp);
                            return (IntPtr)1;
                    }
                }
            }
        }
        return CallNextHookEx(_hookID, nCode, wParam, lParam);
    }

    public static void Main() {
        Console.Title = \"WASD Movement - Ngoc Rong VIAM\";
        Console.ForegroundColor = ConsoleColor.Cyan;
        Console.WriteLine(\"=========================================================\");
        Console.WriteLine(\"   BO DIEU KHIEN DI CHUYEN PHIM W - A - S - D (NRO VIAM) \");
        Console.WriteLine(\"=========================================================\");
        Console.ResetColor();
        Console.WriteLine(\" - Phim W : Bay len   (Up Arrow)\");
        Console.WriteLine(\" - Phim S : Xuong     (Down Arrow)\");
        Console.WriteLine(\" - Phim A : Sang trai (Left Arrow)\");
        Console.WriteLine(\" - Phim D : Sang phai (Right Arrow)\");
        Console.WriteLine(\"---------------------------------------------------------\");
        Console.ForegroundColor = ConsoleColor.Yellow;
        Console.WriteLine(\" * Chi hoat dong khi cua so game DragonBoy dang duoc chon.\");
        Console.WriteLine(\" * Nhan phim [F12] trong game de BAT / TAT nhanh khi can Chat.\");
        Console.WriteLine(\" * Dong cua so nay khi khong choi game nua.\");
        Console.WriteLine(\"---------------------------------------------------------\");
        Console.ForegroundColor = ConsoleColor.Green;
        Console.WriteLine(\"[READY] Dang kich hoat che do di chuyen WASD...\");

        using (Process curProcess = Process.GetCurrentProcess())
        using (ProcessModule curModule = curProcess.MainModule) {
            _hookID = SetWindowsHookEx(WH_KEYBOARD_LL, _proc, GetModuleHandle(curModule.ModuleName), 0);
        }
        Application.Run();
        UnhookWindowsHookEx(_hookID);
    }
}
'@; Add-Type -TypeDefinition $code -ReferencedAssemblies 'System.Windows.Forms'; [WasdMapper]::Main()"

