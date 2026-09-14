#!/bin/bash
# Generic QEMU/KVM VM launcher with Virtio-Venus (virtio-vga-gl) enabled
# https://wiki.cachyos.org/virtualization/virtio-venus
#
# Usage:
#   ./launch-vm.sh                                             # interactive menu
#   ./launch-vm.sh 1                                           # boot distro #1 directly
#   VM_NAME=myvm ./launch-vm.sh                                # boot a custom-named VM
#   VM_NAME=myvm ISO_PATH=/path/to/distro.iso ./launch-vm.sh   # boot installer

set -e

DISTROS=(cachyos ubuntu debian fedora nixos bazzite)

if [ -z "$VM_NAME" ]; then
  selection="$1"
  if [ -z "$selection" ]; then
    echo "Select a VM to launch:"
    for i in "${!DISTROS[@]}"; do
      echo "  $i) ${DISTROS[$i]}"
    done
    read -rp "Selection: " selection
  fi

  if ! [[ "$selection" =~ ^[0-9]+$ ]] || [ "$selection" -ge "${#DISTROS[@]}" ]; then
    echo "Invalid selection: $selection" >&2
    exit 1
  fi

  VM_NAME="${DISTROS[$selection]}"
fi

VM_DIR="${VM_DIR:-$HOME/.local/share/libvirt/images}"
DISK_IMG="${DISK_IMG:-$VM_DIR/$VM_NAME.qcow2}"
DISK_SIZE="${DISK_SIZE:-64G}"
RAM="${RAM:-16G}"
SMP="${SMP:-6}"
HOSTMEM="${HOSTMEM:-4G}"
XRES="${XRES:-2560}"
YRES="${YRES:-1440}"
OVMF_CODE="${OVMF_CODE:-/usr/share/edk2/x64/OVMF_CODE.4m.fd}"
NVRAM_DIR="${NVRAM_DIR:-$HOME/.config/libvirt/qemu/nvram}"
OVMF_VARS="${OVMF_VARS:-$NVRAM_DIR/${VM_NAME}_VARS.fd}"
OVMF_VARS_TEMPLATE="${OVMF_VARS_TEMPLATE:-/usr/share/edk2/x64/OVMF_VARS.4m.fd}"

mkdir -p "$VM_DIR" "$NVRAM_DIR"

if [ ! -f "$DISK_IMG" ]; then
  echo "Creating disk image: $DISK_IMG ($DISK_SIZE)"
  qemu-img create -f qcow2 "$DISK_IMG" "$DISK_SIZE"
fi

if [ ! -f "$OVMF_VARS" ]; then
  echo "Creating UEFI NVRAM vars: $OVMF_VARS"
  cp "$OVMF_VARS_TEMPLATE" "$OVMF_VARS"
fi

CDROM_ARGS=()
if [ -n "$ISO_PATH" ]; then
  if [ ! -f "$ISO_PATH" ]; then
    echo "ISO_PATH ($ISO_PATH) does not exist." >&2
    exit 1
  fi
  echo "Booting with installer attached: $ISO_PATH"

  CDROM_ARGS=(-drive "if=none,id=cdrom0,file=$ISO_PATH,media=cdrom,readonly=on" -device ide-cd,drive=cdrom0,bootindex=1)
fi

exec qemu-system-x86_64 \
  -name "$VM_NAME" \
  -machine q35,accel=kvm,memory-backend=mem1 \
  -cpu host \
  -smp "$SMP" \
  -m "$RAM" \
  -object "memory-backend-memfd,id=mem1,size=$RAM" \
  -drive "if=pflash,format=raw,readonly=on,file=$OVMF_CODE" \
  -drive "if=pflash,format=raw,file=$OVMF_VARS" \
  -drive "if=none,id=disk0,file=$DISK_IMG,cache=writeback,discard=unmap" \
  -device virtio-blk-pci,drive=disk0,bootindex=2 \
  "${CDROM_ARGS[@]}" \
  -net nic,model=virtio \
  -net user \
  -device "virtio-vga-gl,hostmem=$HOSTMEM,blob=true,venus=true,xres=$XRES,yres=$YRES" \
  -vga none \
  -display gtk,gl=on,show-cursor=on \
  -usb -device usb-tablet \
  -d guest_errors
