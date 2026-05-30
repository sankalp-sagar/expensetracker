import { useEffect, useState } from "react";
import { toast } from "sonner";
import { expensesApi } from "@/lib/services";
import { userOptionLabel } from "@/lib/userDisplay";
import {
  Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle,
} from "@/components/ui/dialog";

const SPLIT_TYPES = ["EQUAL", "EXACT", "PERCENTAGE", "SHARE"];

export default function ExpenseFormDialog({
  open,
  onOpenChange,
  group,
  payerId,
  profilesByUserId = {},
  onCreated,
}) {
  const [description, setDescription] = useState("");
  const [amount, setAmount] = useState("");
  const [splitType, setSplitType] = useState("EQUAL");
  const [rows, setRows] = useState([]);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (open && group) {
      const memberRows = (group.members || []).map((m) => ({
        userId: m.userId, value: "",
      }));
      setRows(memberRows.length > 0 ? memberRows : [{ userId: payerId, value: "" }]);
      setDescription("");
      setAmount("");
      setSplitType("EQUAL");
    }
  }, [open, group, payerId]);

  const updateRow = (idx, key, val) =>
    setRows((rs) => rs.map((r, i) => (i === idx ? { ...r, [key]: val } : r)));

  const submit = async (e) => {
    e.preventDefault();
    if (!amount || Number(amount) <= 0) return toast.error("Amount required");
    setSaving(true);
    try {
      const payload = {
        groupId: group.id,
        payerId,
        description,
        amount: Number(amount),
        currency: group.defaultCurrency || "USD",
        splitType,
        splits: rows.map((r) => ({
          userId: r.userId,
          value: splitType === "EQUAL" ? null : Number(r.value || 0),
        })),
      };
      await expensesApi.create(payload);
      toast.success("Expense added");
      onCreated?.();
      onOpenChange(false);
    } catch (err) {
      toast.error(err.response?.data?.message || "Create failed");
    } finally { setSaving(false); }
  };

  const helper = {
    EQUAL: "Total amount split evenly across participants.",
    EXACT: "Specify the exact amount each person owes (must sum to total).",
    PERCENTAGE: "Specify each person's percentage (must sum to 100).",
    SHARE: "Specify weights — e.g. 1, 1, 2 splits in 1:1:2 ratio.",
  }[splitType];

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="rounded-sm max-w-lg" data-testid="expense-form-dialog">
        <DialogHeader>
          <DialogTitle className="font-display">Add expense</DialogTitle>
        </DialogHeader>
        <form onSubmit={submit} className="space-y-4">
          <div className="grid grid-cols-2 gap-3">
            <div className="col-span-2">
              <label className="block text-xs uppercase tracking-wider text-zinc-500 mb-1.5">Description</label>
              <input
                data-testid="expense-description"
                required
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                className="w-full px-3 py-2 border border-zinc-300 rounded-sm text-sm focus:outline-none focus:border-[#0055FF] focus:ring-1 focus:ring-[#0055FF]"
                placeholder="Pizza · Uber · Groceries"
              />
            </div>
            <div>
              <label className="block text-xs uppercase tracking-wider text-zinc-500 mb-1.5">Amount</label>
              <input
                data-testid="expense-amount"
                type="number" step="0.01" min="0.01" required
                value={amount}
                onChange={(e) => setAmount(e.target.value)}
                className="w-full px-3 py-2 border border-zinc-300 rounded-sm font-mono text-sm focus:outline-none focus:border-[#0055FF] focus:ring-1 focus:ring-[#0055FF]"
                placeholder="0.00"
              />
            </div>
            <div>
              <label className="block text-xs uppercase tracking-wider text-zinc-500 mb-1.5">Split type</label>
              <select
                data-testid="expense-split-type"
                value={splitType}
                onChange={(e) => setSplitType(e.target.value)}
                className="w-full px-3 py-2 border border-zinc-300 rounded-sm text-sm focus:outline-none focus:border-[#0055FF] focus:ring-1 focus:ring-[#0055FF]"
              >
                {SPLIT_TYPES.map((t) => <option key={t}>{t}</option>)}
              </select>
            </div>
          </div>

          <p className="text-[11px] text-zinc-500 font-mono">{helper}</p>

          <div className="border border-zinc-200 rounded-sm" data-testid="expense-splits">
            <div className="px-3 py-2 border-b border-zinc-200 flex items-center justify-between bg-zinc-50">
              <span className="text-xs uppercase tracking-wider text-zinc-500">Participants ({rows.length})</span>
            </div>
            <ul>
              {rows.map((r, i) => (
                <li key={i} className="px-3 py-2 flex items-center gap-2 border-b border-zinc-100 last:border-0">
                  <span className="flex-1 text-sm text-zinc-700 truncate">
                    {userOptionLabel(r.userId, profilesByUserId, payerId)}
                  </span>
                  {splitType !== "EQUAL" && (
                    <input
                      data-testid={`split-value-${i}`}
                      type="number" step="0.01"
                      value={r.value}
                      onChange={(e) => updateRow(i, "value", e.target.value)}
                      className="w-24 px-2 py-1 border border-zinc-300 rounded-sm font-mono text-xs text-right focus:outline-none focus:border-[#0055FF]"
                      placeholder={splitType === "PERCENTAGE" ? "%" : splitType === "SHARE" ? "weight" : "$"}
                    />
                  )}
                </li>
              ))}
            </ul>
          </div>

          <DialogFooter>
            <button
              type="submit"
              disabled={saving}
              data-testid="expense-submit"
              className="bg-[#0055FF] hover:bg-[#0044CC] text-white px-4 py-2 rounded-sm text-sm font-medium disabled:opacity-50"
            >
              {saving ? "Saving…" : "Add expense"}
            </button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
