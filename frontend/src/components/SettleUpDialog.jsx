import { useEffect, useState } from "react";
import { toast } from "sonner";
import { settlementsApi } from "@/lib/services";
import { userOptionLabel } from "@/lib/userDisplay";
import {
  Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle,
} from "@/components/ui/dialog";

export default function SettleUpDialog({
  open,
  onOpenChange,
  group,
  currentUserId,
  suggestion,
  profilesByUserId = {},
  onSettled,
}) {
  const [payerId, setPayerId] = useState("");
  const [payeeId, setPayeeId] = useState("");
  const [amount, setAmount] = useState("");
  const [method, setMethod] = useState("CASH");
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (open) {
      if (suggestion) {
        setPayerId(suggestion.from);
        setPayeeId(suggestion.to);
        setAmount(Number(suggestion.amount).toFixed(2));
      } else {
        setPayerId(currentUserId || "");
        const other = (group?.members || []).find((m) => m.userId !== currentUserId);
        setPayeeId(other?.userId || "");
        setAmount("");
      }
      setMethod("CASH");
    }
  }, [open, suggestion, group, currentUserId]);

  const submit = async (e) => {
    e.preventDefault();
    if (!payerId || !payeeId || !amount) return toast.error("All fields required");
    if (payerId === payeeId) return toast.error("Payer and payee must differ");
    if (currentUserId && payerId !== currentUserId && payeeId !== currentUserId) {
      return toast.error("You must be the payer or payee");
    }
    setSaving(true);
    try {
      await settlementsApi.record({
        groupId: group?.id,
        payerId,
        payeeId,
        amount: Number(amount),
        currency: group?.defaultCurrency || "USD",
        method,
      });
      toast.success("Settlement recorded");
      onSettled?.();
      onOpenChange(false);
    } catch (err) {
      toast.error(err.response?.data?.message || "Settle failed");
    } finally { setSaving(false); }
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="rounded-sm" data-testid="settle-up-dialog">
        <DialogHeader>
          <DialogTitle className="font-display">Record payment</DialogTitle>
        </DialogHeader>
        <form onSubmit={submit} className="space-y-3">
          <div>
            <label className="block text-xs uppercase tracking-wider text-zinc-500 mb-1.5">Payer (from)</label>
            <select
              data-testid="settle-payer-select"
              value={payerId}
              onChange={(e) => setPayerId(e.target.value)}
              className="w-full px-3 py-2 border border-zinc-300 rounded-sm text-sm focus:outline-none focus:border-[#0055FF]"
            >
              <option value="">— select —</option>
              {(group?.members || []).map((m) => (
                <option key={m.userId} value={m.userId}>
                  {userOptionLabel(m.userId, profilesByUserId, currentUserId)}
                </option>
              ))}
            </select>
          </div>
          <div>
            <label className="block text-xs uppercase tracking-wider text-zinc-500 mb-1.5">Payee (to)</label>
            <select
              data-testid="settle-payee-select"
              value={payeeId}
              onChange={(e) => setPayeeId(e.target.value)}
              className="w-full px-3 py-2 border border-zinc-300 rounded-sm text-sm focus:outline-none focus:border-[#0055FF]"
            >
              <option value="">— select —</option>
              {(group?.members || []).map((m) => (
                <option key={m.userId} value={m.userId}>
                  {userOptionLabel(m.userId, profilesByUserId, currentUserId)}
                </option>
              ))}
            </select>
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs uppercase tracking-wider text-zinc-500 mb-1.5">Amount</label>
              <input
                data-testid="settle-amount"
                type="number" step="0.01" min="0.01" required
                value={amount}
                onChange={(e) => setAmount(e.target.value)}
                className="w-full px-3 py-2 border border-zinc-300 rounded-sm font-mono text-sm focus:outline-none focus:border-[#0055FF]"
              />
            </div>
            <div>
              <label className="block text-xs uppercase tracking-wider text-zinc-500 mb-1.5">Method</label>
              <select
                data-testid="settle-method"
                value={method}
                onChange={(e) => setMethod(e.target.value)}
                className="w-full px-3 py-2 border border-zinc-300 rounded-sm text-sm focus:outline-none focus:border-[#0055FF]"
              >
                <option>CASH</option>
                <option>BANK_TRANSFER</option>
                <option>UPI</option>
                <option>OTHER</option>
              </select>
            </div>
          </div>
          <DialogFooter>
            <button
              type="submit"
              disabled={saving}
              data-testid="settle-submit"
              className="bg-[#0055FF] hover:bg-[#0044CC] text-white px-4 py-2 rounded-sm text-sm font-medium disabled:opacity-50"
            >
              {saving ? "Recording…" : "Record"}
            </button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
